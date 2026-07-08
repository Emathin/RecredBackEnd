package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.infrastructure.web.SchoolLicenseAccessGuard;
import com.recred.backend.infrastructure.web.SchoolLicenseStudentInterceptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolLicenseStudentInterceptorTest {

    @Mock
    private SchoolLicenseAccessGuard schoolLicenseAccessGuard;

    private SchoolLicenseStudentInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new SchoolLicenseStudentInterceptor(schoolLicenseAccessGuard);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void preHandle_WhenParentRequestHasStudentPathVariable_ShouldRequireStudentLicense() {
        UUID studentId = UUID.randomUUID();
        Authentication authentication = givenAuthenticatedParent();
        MockHttpServletRequest request = requestWithPathVariable("alumnoId", studentId);

        boolean result = whenPreHandling(request);

        assertTrue(result);
        verify(schoolLicenseAccessGuard).isAuthenticatedParent(authentication);
        verify(schoolLicenseAccessGuard).requireActiveSchoolForStudent(studentId);
    }

    @Test
    void preHandle_WhenParentRequestHasStudentQueryParam_ShouldRequireStudentLicense() {
        UUID studentId = UUID.randomUUID();
        givenAuthenticatedParent();
        MockHttpServletRequest request = requestWithQueryParam("studentId", studentId);

        boolean result = whenPreHandling(request);

        assertTrue(result);
        verify(schoolLicenseAccessGuard).requireActiveSchoolForStudent(studentId);
    }

    @Test
    void preHandle_WhenAuthenticatedUserIsNotParent_ShouldNotRequireStudentLicense() {
        UUID studentId = UUID.randomUUID();
        givenAuthenticatedNonParent();
        MockHttpServletRequest request = requestWithPathVariable("alumnoId", studentId);

        boolean result = whenPreHandling(request);

        assertTrue(result);
        verify(schoolLicenseAccessGuard, never()).requireActiveSchoolForStudent(studentId);
    }

    @Test
    void preHandle_WhenParentRequestHasNoStudentReference_ShouldNotRequireStudentLicense() {
        givenAuthenticatedUser();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/tutores/me");

        boolean result = whenPreHandling(request);

        assertTrue(result);
        verify(schoolLicenseAccessGuard, never()).isAuthenticatedParent(
                org.mockito.ArgumentMatchers.any());
        verify(schoolLicenseAccessGuard, never()).requireActiveSchoolForStudent(
                org.mockito.ArgumentMatchers.any());
    }

    private Authentication givenAuthenticatedParent() {
        Authentication authentication = authentication();
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(schoolLicenseAccessGuard.isAuthenticatedParent(authentication)).thenReturn(true);
        return authentication;
    }

    private void givenAuthenticatedNonParent() {
        Authentication authentication = authentication();
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(schoolLicenseAccessGuard.isAuthenticatedParent(authentication)).thenReturn(false);
    }

    private void givenAuthenticatedUser() {
        SecurityContextHolder.getContext().setAuthentication(authentication());
    }

    private boolean whenPreHandling(MockHttpServletRequest request) {
        try {
            return interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private MockHttpServletRequest requestWithPathVariable(String name, UUID value) {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/api/v1/alumnos/" + value
        );
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
                Map.of(name, value.toString()));
        return request;
    }

    private MockHttpServletRequest requestWithQueryParam(String name, UUID value) {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/api/v1/wallets"
        );
        request.addParameter(name, value.toString());
        return request;
    }

    private Authentication authentication() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                "cognito-subject",
                null
        );
        authentication.setAuthenticated(true);
        return authentication;
    }
}
