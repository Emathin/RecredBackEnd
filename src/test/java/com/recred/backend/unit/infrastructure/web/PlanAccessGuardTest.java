package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.domain.exceptions.UsuarioNoEncontradoException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.TipoPlan;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.infrastructure.web.PlanAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.recred.backend.domain.models.RolUsuario.ADMIN;
import static com.recred.backend.domain.models.RolUsuario.PADRE;
import static com.recred.backend.domain.models.RolUsuario.VENDEDOR;
import static com.recred.backend.domain.models.TipoPlan.AVANZADO;
import static com.recred.backend.domain.models.TipoPlan.GRATUITO;
import static com.recred.backend.domain.models.TipoPlan.INTERMEDIO;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanAccessGuardTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private TutorRepository tutorRepository;

    private PlanAccessGuard guard;
    private UUID usuarioId;
    private UUID alumnoId;

    @BeforeEach
    void setUp() {
        guard = new PlanAccessGuard(usuarioRepository, tutorRepository);
        usuarioId = UUID.randomUUID();
        alumnoId = UUID.randomUUID();
    }

    @Test
    void requireAtLeastWhenUserHasHigherPlanShouldAllowAccess() {
        givenUserWithPlan(AVANZADO);

        Executable execution = whenRequiringAtLeast(INTERMEDIO);

        thenExecutionSucceeds(execution);
    }

    @Test
    void requireAtLeastWhenUserHasSamePlanShouldAllowAccess() {
        givenUserWithPlan(INTERMEDIO);

        Executable execution = whenRequiringAtLeast(INTERMEDIO);

        thenExecutionSucceeds(execution);
    }

    @Test
    void requireAtLeastWhenUserHasLowerPlanShouldDenyAccess() {
        givenUserWithPlan(GRATUITO);

        Executable execution = whenRequiringAtLeast(INTERMEDIO);

        thenThrowsAccessDeniedException(execution);
    }

    @Test
    void requireAdvancedWhenUserHasIntermediatePlanShouldDenyAccess() {
        givenUserWithPlan(INTERMEDIO);

        Executable execution = whenRequiringAdvanced();

        thenThrowsAccessDeniedException(execution);
    }

    @Test
    void requireAtLeastWhenUserDoesNotExistShouldThrowException() {
        givenUserDoesNotExist();

        Executable execution = whenRequiringAtLeast(INTERMEDIO);

        thenThrowsUsuarioNoEncontradoException(execution);
    }

    @Test
    void requireVendorAtLeastWhenVendorHasRequiredPlanShouldAllowAccess() {
        givenAuthenticatedUser(user(INTERMEDIO, VENDEDOR));

        Executable execution = whenRequiringVendorIntermediate();

        thenExecutionSucceeds(execution);
    }

    @Test
    void requireVendorAtLeastWhenVendorHasLowerPlanShouldDenyAccess() {
        givenAuthenticatedUser(user(GRATUITO, VENDEDOR));

        Executable execution = whenRequiringVendorIntermediate();

        thenThrowsAccessDeniedException(execution);
    }

    @Test
    void requireVendorAtLeastWhenUserIsNotVendorShouldDenyAccess() {
        givenAuthenticatedUser(user(AVANZADO, PADRE));

        Executable execution = whenRequiringVendorAdvanced();

        thenThrowsAccessDeniedException(execution);
    }

    @Test
    void requireVendorAtLeastWhenUserIsAdminShouldAllowAccess() {
        givenAuthenticatedUser(user(GRATUITO, ADMIN));

        Executable execution = whenRequiringVendorAdvanced();

        thenExecutionSucceeds(execution);
    }

    @Test
    void requireVendorAtLeastWhenAuthenticationIsMissingShouldThrowException() {
        Executable execution = whenRequiringVendorIntermediateWithoutAuth();

        thenThrowsAuthenticationCredentialsNotFoundException(execution);
    }

    @Test
    void requireVendorAtLeastWhenAuthenticatedUserDoesNotExistShouldThrowException() {
        givenAuthenticatedUserNotFound();

        Executable execution = whenRequiringVendorIntermediate();

        thenThrowsAuthenticationCredentialsNotFoundException(execution);
    }

    @Test
    void requireTutorAtLeastWhenTutorHasRequiredPlanShouldAllowAccess() {
        givenAuthenticatedUser(user(INTERMEDIO, PADRE));

        Executable execution = whenRequiringTutorIntermediate();

        thenExecutionSucceeds(execution);
    }

    @Test
    void requireStudentInheritedTutorAtLeastWhenTutorHasRequiredPlanShouldAllowAccess() {
        givenAuthenticatedStudentUserAndTutorWithPlan(AVANZADO);

        Executable execution = whenRequiringStudentInheritedTutorAdvanced();

        thenExecutionSucceeds(execution);
    }

    @Test
    void requireStudentInheritedTutorAtLeastWhenTutorHasLowerPlanShouldDenyAccess() {
        givenAuthenticatedStudentUserAndTutorWithPlan(INTERMEDIO);

        Executable execution = whenRequiringStudentInheritedTutorAdvanced();

        thenThrowsAccessDeniedException(execution);
    }

    private void givenUserWithPlan(TipoPlan plan) {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(user(plan)));
    }

    private void givenUserDoesNotExist() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());
    }

    private void givenAuthenticatedUser(Usuario user) {
        when(usuarioRepository.findByIdCognito("vendor-sub")).thenReturn(Optional.of(user));
    }

    private void givenAuthenticatedUserNotFound() {
        when(usuarioRepository.findByIdCognito("vendor-sub")).thenReturn(Optional.empty());
    }

    private void givenAuthenticatedStudentUserAndTutorWithPlan(TipoPlan plan) {
        Usuario alumno = studentUser();
        givenAuthenticatedUser(alumno);
        when(tutorRepository.findByAlumnoId(alumnoId)).thenReturn(List.of(tutorWithPlan(plan)));
    }

    private Executable whenRequiringAtLeast(TipoPlan minimumPlan) {
        return () -> guard.requireAtLeast(usuarioId, minimumPlan);
    }

    private Executable whenRequiringAdvanced() {
        return () -> guard.requireAdvanced(usuarioId);
    }

    private Executable whenRequiringVendorIntermediate() {
        return () -> guard.requireVendorIntermediate(authentication());
    }

    private Executable whenRequiringVendorAdvanced() {
        return () -> guard.requireVendorAdvanced(authentication());
    }

    private Executable whenRequiringVendorIntermediateWithoutAuth() {
        return () -> guard.requireVendorIntermediate(null);
    }

    private Executable whenRequiringTutorIntermediate() {
        return () -> guard.requireTutorIntermediate(authentication());
    }

    private Executable whenRequiringStudentInheritedTutorAdvanced() {
        return () -> guard.requireStudentInheritedTutorAdvanced(authentication());
    }

    private void thenExecutionSucceeds(Executable execution) {
        assertDoesNotThrow(execution);
    }

    private void thenThrowsAccessDeniedException(Executable execution) {
        assertThrows(AccessDeniedException.class, execution);
    }

    private void thenThrowsUsuarioNoEncontradoException(Executable execution) {
        assertThrows(UsuarioNoEncontradoException.class, execution);
    }

    private void thenThrowsAuthenticationCredentialsNotFoundException(Executable execution) {
        assertThrows(AuthenticationCredentialsNotFoundException.class, execution);
    }

    private Usuario user(TipoPlan plan) {
        return user(plan, null);
    }

    private Usuario user(TipoPlan plan, RolUsuario role) {
        Usuario user = new Usuario();
        user.setId(usuarioId);
        user.setPlan(plan);
        user.setRol(role);
        return user;
    }

    private Usuario studentUser() {
        Usuario user = user(GRATUITO, RolUsuario.ALUMNO);
        Alumno alumno = new Alumno();
        alumno.setId(alumnoId);
        user.setAlumno(alumno);
        return user;
    }

    private Tutor tutorWithPlan(TipoPlan plan) {
        Usuario user = new Usuario();
        user.setId(UUID.randomUUID());
        user.setPlan(plan);
        Tutor tutor = new Tutor();
        tutor.setUsuario(user);
        return tutor;
    }

    private Authentication authentication() {
        return new TestingAuthenticationToken("vendor-sub", "");
    }
}
