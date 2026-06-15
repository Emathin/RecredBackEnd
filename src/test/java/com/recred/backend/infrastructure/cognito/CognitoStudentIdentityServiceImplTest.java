package com.recred.backend.infrastructure.cognito;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.config.CognitoAdminProperties;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CognitoStudentIdentityServiceImplTest {

    @Test
    void createStudentUser_WhenRequiredConfigIsMissing_ShouldFailBeforeHttpCall() {
        CognitoStudentIdentityServiceImpl service = service(new CognitoAdminProperties());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.createStudentUser("mateo", "m@recred.test", "Mateo", "Perez"));

        assertTrue(exception.getMessage().contains("userPoolId"));
    }

    @Test
    void credentialsFromProperties_ShouldTrimValuesAndIgnoreBlankSessionToken() {
        CognitoAdminProperties properties = properties();
        properties.setAwsSessionToken(" ");
        CognitoStudentIdentityServiceImpl service = service(properties);

        Object credentials = ReflectionTestUtils.invokeMethod(
                service,
                "credentialsFromProperties"
        );

        assertNotNull(credentials);
        assertEquals("access-key", ReflectionTestUtils.invokeMethod(
                credentials,
                "accessKeyId"
        ));
        assertEquals("secret-key", ReflectionTestUtils.invokeMethod(
                credentials,
                "secretAccessKey"
        ));
        assertNull(ReflectionTestUtils.invokeMethod(credentials, "sessionToken"));
    }

    @Test
    void authorizationHeader_WithoutSessionToken_ShouldSignExpectedHeaders()
            throws Exception {
        CognitoStudentIdentityServiceImpl service = service(properties());
        Object credentials = ReflectionTestUtils.invokeMethod(
                service,
                "credentialsFromProperties"
        );

        String authorization = ReflectionTestUtils.invokeMethod(
                service,
                "authorizationHeader",
                credentials,
                "us-east-1",
                "AWSCognitoIdentityProviderService.AdminCreateUser",
                "{\"test\":true}",
                "cognito-idp.us-east-1.amazonaws.com",
                "20260615T120000Z",
                "20260615"
        );

        assertNotNull(authorization);
        assertTrue(authorization.contains("AWS4-HMAC-SHA256 Credential=access-key"));
        assertTrue(authorization.contains("content-type;host;x-amz-date;x-amz-target"));
        assertTrue(authorization.contains("Signature="));
    }

    @Test
    void authorizationHeader_WithSessionToken_ShouldIncludeSecurityTokenHeader()
            throws Exception {
        CognitoAdminProperties properties = properties();
        properties.setAwsSessionToken("session-token");
        CognitoStudentIdentityServiceImpl service = service(properties);
        Object credentials = ReflectionTestUtils.invokeMethod(
                service,
                "credentialsFromProperties"
        );

        String authorization = ReflectionTestUtils.invokeMethod(
                service,
                "authorizationHeader",
                credentials,
                "us-east-1",
                "AWSCognitoIdentityProviderService.AdminCreateUser",
                "{\"test\":true}",
                "cognito-idp.us-east-1.amazonaws.com",
                "20260615T120000Z",
                "20260615"
        );

        assertNotNull(authorization);
        assertTrue(authorization.contains(
                "content-type;host;x-amz-date;x-amz-security-token;x-amz-target"
        ));
    }

    @Test
    void extractSub_ShouldReturnSubAttribute() {
        CognitoStudentIdentityServiceImpl service = service(properties());

        String sub = ReflectionTestUtils.invokeMethod(service, "extractSub", Map.of(
                "Username", "student",
                "Attributes", List.of(
                        Map.of("Name", "email", "Value", "student@recred.test"),
                        Map.of("Name", "sub", "Value", "sub-123")
                )
        ));

        assertEquals("sub-123", sub);
    }

    @Test
    void extractSub_WhenAttributesAreMissing_ShouldFail() {
        CognitoStudentIdentityServiceImpl service = service(properties());

        assertThrows(IllegalStateException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "extractSub", Map.of()));
        assertThrows(IllegalStateException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "extractSub", Map.of(
                        "Attributes",
                        List.of(Map.of("Name", "email", "Value", "student@recred.test"))
                )));
    }

    @Test
    void attributeAndStringValue_ShouldMapCognitoValues() {
        CognitoStudentIdentityServiceImpl service = service(properties());

        Map<String, String> attribute = ReflectionTestUtils.invokeMethod(
                service,
                "attribute",
                "email",
                "student@recred.test"
        );

        assertEquals("email", attribute.get("Name"));
        assertEquals("student@recred.test", attribute.get("Value"));
        assertEquals("123", ReflectionTestUtils.invokeMethod(service, "stringValue", 123));
        assertNull(ReflectionTestUtils.invokeMethod(service, "stringValue", (Object) null));
    }

    @Test
    void hashes_ShouldReturnExpectedLengths() {
        CognitoStudentIdentityServiceImpl service = service(properties());

        String sha256 = ReflectionTestUtils.invokeMethod(service, "sha256Hex", "payload");
        String hmac = ReflectionTestUtils.invokeMethod(
                service,
                "hmacHex",
                "key".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                "payload"
        );

        assertEquals(64, sha256.length());
        assertEquals(64, hmac.length());
    }

    private CognitoStudentIdentityServiceImpl service(CognitoAdminProperties properties) {
        return new CognitoStudentIdentityServiceImpl(properties, new ObjectMapper());
    }

    private CognitoAdminProperties properties() {
        CognitoAdminProperties properties = new CognitoAdminProperties();
        properties.setUserPoolId("pool");
        properties.setRegion("us-east-1");
        properties.setAwsAccessKeyId(" access-key ");
        properties.setAwsSecretAccessKey(" secret-key ");
        return properties;
    }
}
