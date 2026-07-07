package com.recred.backend.infrastructure.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConditionalOnBean(SchoolLicenseAccessGuard.class)
public class SchoolLicenseWebConfig implements WebMvcConfigurer {

    private final SchoolLicenseAccessGuard schoolLicenseAccessGuard;

    public SchoolLicenseWebConfig(SchoolLicenseAccessGuard schoolLicenseAccessGuard) {
        this.schoolLicenseAccessGuard = schoolLicenseAccessGuard;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SchoolLicenseStudentInterceptor(schoolLicenseAccessGuard))
                .addPathPatterns("/api/v1/**");
    }
}
