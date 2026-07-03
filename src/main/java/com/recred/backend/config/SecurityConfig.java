package com.recred.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

        private static final List<String> ALLOWED_METHODS = List.of(
                HttpMethod.GET.name(), HttpMethod.POST.name(), HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(), HttpMethod.DELETE.name(), HttpMethod.OPTIONS.name());
        private static final long CORS_MAX_AGE_SECONDS = 3600L;

        @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
        private String issuerUri;

        @Value("${spring.security.oauth2.resourceserver.jwt.audiences}")
        private String clientId;

        private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
        private final CustomJwtAuthenticationConverter customJwtAuthenticationConverter;

        public SecurityConfig(CustomAuthenticationEntryPoint customAuthenticationEntryPoint,
                              CustomJwtAuthenticationConverter customJwtAuthenticationConverter) {
                this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;
                this.customJwtAuthenticationConverter = customJwtAuthenticationConverter;
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                        .cors(Customizer.withDefaults())
                        .csrf(csrf -> csrf.disable())
                        .sessionManagement(session ->
                                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                        .authorizeHttpRequests(auth -> auth
                                .requestMatchers("/ping", "/actuator/health").permitAll()
                                .requestMatchers("/api/v1/school-registrations").permitAll()
                                .requestMatchers("/swagger-ui.html",
                                        "/swagger-ui/**",
                                        "/v3/api-docs",
                                        "/v3/api-docs/**",
                                        "/v3/api-docs.yaml"
                                        )
                                .permitAll()
                                .anyRequest()
                                .authenticated()
                        )

                        .oauth2ResourceServer(oauth2 -> oauth2
                                .authenticationEntryPoint(customAuthenticationEntryPoint)
                                .jwt(jwt -> jwt
                                        .decoder(jwtDecoder())
                                        .jwtAuthenticationConverter
                                                (customJwtAuthenticationConverter)
                                )
                        );

                return http.build();
        }

        @Bean
        public JwtDecoder jwtDecoder() {
                NimbusJwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation(issuerUri);

                OAuth2TokenValidator<Jwt> withIssuer = JwtValidators
                        .createDefaultWithIssuer(issuerUri);
                OAuth2TokenValidator<Jwt> withAudience = new CognitoAudienceValidator(clientId);

                OAuth2TokenValidator<Jwt> validator = new
                        DelegatingOAuth2TokenValidator<>(withIssuer, withAudience);
                jwtDecoder.setJwtValidator(validator);

                return jwtDecoder;
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOriginPatterns(corsProperties.allowedOriginPatterns());
                configuration.setAllowedMethods(ALLOWED_METHODS);
                configuration.setAllowedHeaders(List.of(CorsConfiguration.ALL));
                configuration.setExposedHeaders(List.of(RequestIdContext.HEADER_NAME));
                configuration.setAllowCredentials(false);
                configuration.setMaxAge(CORS_MAX_AGE_SECONDS);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);

                return source;
        }
}
