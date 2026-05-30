package com.recred.backend.config;

import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

        private static final List<String> ALLOWED_METHODS = List.of(
                        HttpMethod.GET.name(),
                        HttpMethod.POST.name(),
                        HttpMethod.PUT.name(),
                        HttpMethod.PATCH.name(),
                        HttpMethod.DELETE.name(),
                        HttpMethod.OPTIONS.name());
        private static final long CORS_MAX_AGE_SECONDS = 3600L;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .cors(Customizer.withDefaults())

                                .csrf(csrf -> csrf.disable())

                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/ping").permitAll()
                                                .requestMatchers("/actuator/health").permitAll()
                                                .requestMatchers(
                                                                "/swagger-ui.html",
                                                                "/swagger-ui/**",
                                                                "/v3/api-docs",
                                                                "/v3/api-docs/**",
                                                                "/api/v1/**",
                                                                "/v3/api-docs.yaml")
                                                .permitAll()
                                                .anyRequest().authenticated())

                                .oauth2ResourceServer(oauth2 -> oauth2
                                                .jwt(Customizer.withDefaults()));

                return http.build();
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
