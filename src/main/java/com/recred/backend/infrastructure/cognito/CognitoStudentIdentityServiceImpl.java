package com.recred.backend.infrastructure.cognito;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.config.CognitoAdminProperties;
import com.recred.backend.domain.services.CognitoStudentIdentityService;
import com.recred.backend.domain.services.CreatedCognitoUser;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
public class CognitoStudentIdentityServiceImpl implements CognitoStudentIdentityService {

    private static final String SERVICE = "cognito-idp";
    private static final String ALGORITHM = "AWS4-HMAC-SHA256";
    private static final String CREATE_USER_TARGET =
            "AWSCognitoIdentityProviderService.AdminCreateUser";
    private static final String ADD_TO_GROUP_TARGET =
            "AWSCognitoIdentityProviderService.AdminAddUserToGroup";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final CognitoAdminProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Clock clock;

    public CognitoStudentIdentityServiceImpl(CognitoAdminProperties properties,
                                             ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
        this.clock = Clock.systemUTC();
    }

    @Override
    public CreatedCognitoUser createStudentUser(String username, String email,
                                                String nombre, String apellido) {
        String userPoolId = required(properties.getUserPoolId(), "userPoolId");
        String region = required(properties.getRegion(), "region");
        String cognitoUsername = username != null && !username.isBlank()
                ? username.trim()
                : "student-" + UUID.randomUUID();

        Map<String, Object> response = invoke(region, CREATE_USER_TARGET, Map.of(
                "UserPoolId", userPoolId,
                "Username", cognitoUsername,
                "DesiredDeliveryMediums", List.of("EMAIL"),
                "UserAttributes", List.of(
                        attribute("email", email),
                        attribute("given_name", nombre),
                        attribute("family_name", apellido)
                )
        ));

        Map<String, Object> user = castMap(response.get("User"));
        String createdUsername = stringValue(user.get("Username"));
        String sub = extractSub(user);

        addToStudentGroupIfConfigured(region, userPoolId, createdUsername);

        return new CreatedCognitoUser(sub, createdUsername);
    }

    private void addToStudentGroupIfConfigured(String region, String userPoolId, String username) {
        String group = properties.getStudentGroup();
        if (group == null || group.isBlank()) {
            return;
        }

        invoke(region, ADD_TO_GROUP_TARGET, Map.of(
                "UserPoolId", userPoolId,
                "Username", username,
                "GroupName", group.trim()
        ));
    }

    private Map<String, Object> invoke(String region, String target, Map<String, Object> body) {
        try {
            String payload = objectMapper.writeValueAsString(body);
            AwsCredentials credentials = credentialsFromProperties();
            URI endpoint = URI.create("https://" + SERVICE + "." + region + ".amazonaws.com/");
            ZonedDateTime now = ZonedDateTime.now(clock);
            String amzDate = now.format(DateTimeFormatter.ofPattern(
                    "yyyyMMdd'T'HHmmss'Z'", Locale.ROOT));
            String dateStamp = now.format(DateTimeFormatter.ofPattern("yyyyMMdd", Locale.ROOT));

            String authorization = authorizationHeader(
                    credentials,
                    region,
                    target,
                    payload,
                    endpoint.getHost(),
                    amzDate,
                    dateStamp
            );

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(endpoint)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .header("Authorization", authorization)
                    .header("Content-Type", "application/x-amz-json-1.1")
                    .header("X-Amz-Date", amzDate)
                    .header("X-Amz-Target", target);

            if (credentials.sessionToken() != null) {
                requestBuilder.header("X-Amz-Security-Token", credentials.sessionToken());
            }

            HttpResponse<String> response = httpClient.send(requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Cognito respondio "
                        + response.statusCode() + ": " + response.body());
            }

            if (response.body() == null || response.body().isBlank()) {
                return Collections.emptyMap();
            }

            return objectMapper.readValue(response.body(), MAP_TYPE);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Se interrumpio la llamada a Cognito", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo crear el usuario en Cognito", ex);
        }
    }

    private String authorizationHeader(AwsCredentials credentials, String region, String target,
                                       String payload, String host, String amzDate,
                                       String dateStamp) throws Exception {
        String credentialScope = dateStamp + "/" + region + "/" + SERVICE + "/aws4_request";
        String signedHeaders = credentials.sessionToken() == null
                ? "content-type;host;x-amz-date;x-amz-target"
                : "content-type;host;x-amz-date;x-amz-security-token;x-amz-target";
        String canonicalHeaders = "content-type:application/x-amz-json-1.1\n"
                + "host:" + host + "\n"
                + "x-amz-date:" + amzDate + "\n"
                + securityTokenHeader(credentials)
                + "x-amz-target:" + target + "\n";
        String canonicalRequest = "POST\n/\n\n"
                + canonicalHeaders + "\n"
                + signedHeaders + "\n"
                + sha256Hex(payload);
        String stringToSign = ALGORITHM + "\n"
                + amzDate + "\n"
                + credentialScope + "\n"
                + sha256Hex(canonicalRequest);
        byte[] signingKey = signingKey(credentials.secretAccessKey(), dateStamp, region);
        String signature = hmacHex(signingKey, stringToSign);

        return ALGORITHM
                + " Credential=" + credentials.accessKeyId() + "/" + credentialScope
                + ", SignedHeaders=" + signedHeaders
                + ", Signature=" + signature;
    }

    private String securityTokenHeader(AwsCredentials credentials) {
        if (credentials.sessionToken() == null) {
            return "";
        }
        return "x-amz-security-token:" + credentials.sessionToken() + "\n";
    }

    private byte[] signingKey(String secretAccessKey, String dateStamp, String region)
            throws Exception {
        byte[] dateKey = hmac(("AWS4" + secretAccessKey).getBytes(StandardCharsets.UTF_8),
                dateStamp);
        byte[] dateRegionKey = hmac(dateKey, region);
        byte[] dateRegionServiceKey = hmac(dateRegionKey, SERVICE);
        return hmac(dateRegionServiceKey, "aws4_request");
    }

    private String extractSub(Map<String, Object> user) {
        Object attributesRaw = user.get("Attributes");
        if (!(attributesRaw instanceof List<?> attributes)) {
            throw new IllegalStateException("Cognito no devolvio atributos del usuario creado");
        }

        return attributes.stream()
                .filter(Map.class::isInstance)
                .map(item -> castMap(item))
                .filter(attribute -> "sub".equals(attribute.get("Name")))
                .map(attribute -> stringValue(attribute.get("Value")))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Cognito no devolvio el sub del usuario creado"));
    }

    private AwsCredentials credentialsFromProperties() {
        String accessKeyId = required(properties.getAwsAccessKeyId(), "aws-access-key-id");
        String secretAccessKey = required(properties.getAwsSecretAccessKey(),
                "aws-secret-access-key");
        String sessionToken = properties.getAwsSessionToken();
        if (sessionToken != null && sessionToken.isBlank()) {
            sessionToken = null;
        }
        return new AwsCredentials(accessKeyId, secretAccessKey, sessionToken);
    }

    private Map<String, String> attribute(String name, String value) {
        return Map.of("Name", name, "Value", value);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    private String sha256Hex(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private String hmacHex(byte[] key, String value) throws Exception {
        return HexFormat.of().formatHex(hmac(key, value));
    }

    private byte[] hmac(byte[] key, String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Falta configurar recred.cognito." + field);
        }
        return value.trim();
    }

    private record AwsCredentials(
            String accessKeyId,
            String secretAccessKey,
            String sessionToken
    ) {
    }
}
