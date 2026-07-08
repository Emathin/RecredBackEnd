package com.recred.backend.unit.application;

import com.recred.backend.application.SchoolAssistantContextProvider;
import com.recred.backend.application.SchoolAssistantContextResolver;
import com.recred.backend.application.SchoolAssistantConversationOwner;
import com.recred.backend.application.SchoolAssistantResolvedContext;
import com.recred.backend.domain.exceptions.UsuarioAutenticadoNoEncontradoException;
import com.recred.backend.domain.models.SchoolAssistantContext;
import com.recred.backend.domain.models.SchoolAssistantRole;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolAssistantContextResolverTest {

    private static final String COGNITO_SUBJECT = "student-sub";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SchoolAssistantContextProvider studentProvider;

    @Mock
    private SchoolAssistantContextProvider tutorProvider;

    private SchoolAssistantContextResolver resolver;
    private UUID userId;
    private Usuario user;
    private SchoolAssistantResolvedContext expectedContext;
    private SchoolAssistantResolvedContext resolvedContext;
    private Executable execution;

    @BeforeEach
    void setUp() {
        resolver = new SchoolAssistantContextResolver(
                usuarioRepository,
                List.of(studentProvider, tutorProvider)
        );
        userId = UUID.randomUUID();
        user = user(userId);
    }

    @Test
    void resolve_WhenProviderSupportsRole_ShouldAuthenticateAndDelegateResolution() {
        givenAuthenticatedUser();
        givenStudentProviderSupportsRole();
        givenStudentProviderResolvesContext();

        whenResolvingStudentContext();

        thenAuthenticatedUserWasRequested();
        thenStudentProviderWasUsed();
        thenResolvedContextIsExpected();
    }

    @Test
    void resolve_WhenUserDoesNotExist_ShouldThrowException() {
        givenAuthenticatedUserDoesNotExist();

        whenResolvingStudentContextFails();

        thenAuthenticatedUserNotFoundExceptionIsThrown();
    }

    @Test
    void resolve_WhenRoleHasNoProvider_ShouldThrowException() {
        givenAuthenticatedUser();
        givenNoProviderSupportsRole();

        whenResolvingStudentContextFails();

        thenUnsupportedRoleExceptionIsThrown();
    }

    private void givenAuthenticatedUser() {
        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT))
                .thenReturn(Optional.of(user));
    }

    private void givenAuthenticatedUserDoesNotExist() {
        when(usuarioRepository.findByIdCognito(COGNITO_SUBJECT))
                .thenReturn(Optional.empty());
    }

    private void givenStudentProviderSupportsRole() {
        when(studentProvider.supports(SchoolAssistantRole.ALUMNO)).thenReturn(true);
    }

    private void givenNoProviderSupportsRole() {
        when(studentProvider.supports(SchoolAssistantRole.ALUMNO)).thenReturn(false);
        when(tutorProvider.supports(SchoolAssistantRole.ALUMNO)).thenReturn(false);
    }

    private void givenStudentProviderResolvesContext() {
        expectedContext = new SchoolAssistantResolvedContext(
                studentContext(),
                SchoolAssistantConversationOwner.student(userId)
        );
        when(studentProvider.resolve(user, COGNITO_SUBJECT)).thenReturn(expectedContext);
    }

    private void whenResolvingStudentContext() {
        resolvedContext = resolver.resolve(SchoolAssistantRole.ALUMNO, COGNITO_SUBJECT);
    }

    private void whenResolvingStudentContextFails() {
        execution = () -> resolver.resolve(SchoolAssistantRole.ALUMNO, COGNITO_SUBJECT);
    }

    private void thenAuthenticatedUserWasRequested() {
        verify(usuarioRepository).findByIdCognito(COGNITO_SUBJECT);
    }

    private void thenStudentProviderWasUsed() {
        verify(studentProvider).resolve(user, COGNITO_SUBJECT);
    }

    private void thenResolvedContextIsExpected() {
        assertSame(expectedContext, resolvedContext);
    }

    private void thenAuthenticatedUserNotFoundExceptionIsThrown() {
        assertThrows(UsuarioAutenticadoNoEncontradoException.class, execution);
    }

    private void thenUnsupportedRoleExceptionIsThrown() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                execution
        );
        assertEquals("unsupported school assistant role", exception.getMessage());
    }

    private SchoolAssistantContext studentContext() {
        return new SchoolAssistantContext(
                userId,
                Optional.empty(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private Usuario user(UUID userId) {
        Usuario user = new Usuario();
        user.setId(userId);
        return user;
    }
}
