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
    void requireAtLeast_WhenUserHasHigherPlan_ShouldAllowAccess() {
        givenUserWithPlan(TipoPlan.AVANZADO);

        Executable execution = () -> whenRequiringAtLeast(TipoPlan.INTERMEDIO);

        assertDoesNotThrow(execution);
    }

    @Test
    void requireAtLeast_WhenUserHasSamePlan_ShouldAllowAccess() {
        givenUserWithPlan(TipoPlan.INTERMEDIO);

        Executable execution = () -> whenRequiringAtLeast(TipoPlan.INTERMEDIO);

        assertDoesNotThrow(execution);
    }

    @Test
    void requireAtLeast_WhenUserHasLowerPlan_ShouldDenyAccess() {
        givenUserWithPlan(TipoPlan.GRATUITO);

        Executable execution = () -> whenRequiringAtLeast(TipoPlan.INTERMEDIO);

        assertThrows(AccessDeniedException.class, execution);
    }

    @Test
    void requireAdvanced_WhenUserHasIntermediatePlan_ShouldDenyAccess() {
        givenUserWithPlan(TipoPlan.INTERMEDIO);

        Executable execution = () -> whenRequiringAdvanced();

        assertThrows(AccessDeniedException.class, execution);
    }

    @Test
    void requireAtLeast_WhenUserDoesNotExist_ShouldThrowException() {
        givenUserDoesNotExist();

        Executable execution = () -> whenRequiringAtLeast(TipoPlan.INTERMEDIO);

        assertThrows(UsuarioNoEncontradoException.class, execution);
    }

    @Test
    void requireVendorAtLeast_WhenVendorHasRequiredPlan_ShouldAllowAccess() {
        givenAuthenticatedUser(user(TipoPlan.INTERMEDIO, RolUsuario.VENDEDOR));

        Executable execution = () -> whenRequiringVendorIntermediate();

        assertDoesNotThrow(execution);
    }

    @Test
    void requireVendorAtLeast_WhenVendorHasLowerPlan_ShouldDenyAccess() {
        givenAuthenticatedUser(user(TipoPlan.GRATUITO, RolUsuario.VENDEDOR));

        Executable execution = () -> whenRequiringVendorIntermediate();

        assertThrows(AccessDeniedException.class, execution);
    }

    @Test
    void requireVendorAtLeast_WhenUserIsNotVendor_ShouldDenyAccess() {
        givenAuthenticatedUser(user(TipoPlan.AVANZADO, RolUsuario.PADRE));

        Executable execution = () -> whenRequiringVendorAdvanced();

        assertThrows(AccessDeniedException.class, execution);
    }

    @Test
    void requireVendorAtLeast_WhenUserIsAdmin_ShouldAllowAccess() {
        givenAuthenticatedUser(user(TipoPlan.GRATUITO, RolUsuario.ADMIN));

        Executable execution = () -> whenRequiringVendorAdvanced();

        assertDoesNotThrow(execution);
    }

    @Test
    void requireVendorAtLeast_WhenAuthenticationIsMissing_ShouldThrowException() {
        Executable execution = () -> guard.requireVendorIntermediate(null);

        assertThrows(AuthenticationCredentialsNotFoundException.class, execution);
    }

    @Test
    void requireVendorAtLeast_WhenAuthenticatedUserDoesNotExist_ShouldThrowException() {
        when(usuarioRepository.findByIdCognito("vendor-sub")).thenReturn(Optional.empty());

        Executable execution = () -> whenRequiringVendorIntermediate();

        assertThrows(AuthenticationCredentialsNotFoundException.class, execution);
    }

    @Test
    void requireTutorAtLeast_WhenTutorHasRequiredPlan_ShouldAllowAccess() {
        givenAuthenticatedUser(user(TipoPlan.INTERMEDIO, RolUsuario.PADRE));

        Executable execution = () -> guard.requireTutorIntermediate(authentication());

        assertDoesNotThrow(execution);
    }

    @Test
    void requireStudentInheritedTutorAtLeast_WhenTutorHasRequiredPlan_ShouldAllowAccess() {
        Usuario alumno = studentUser();
        givenAuthenticatedUser(alumno);
        when(tutorRepository.findByAlumnoId(alumnoId))
                .thenReturn(List.of(tutorWithPlan(TipoPlan.AVANZADO)));

        Executable execution =
                () -> guard.requireStudentInheritedTutorAdvanced(authentication());

        assertDoesNotThrow(execution);
    }

    @Test
    void requireStudentInheritedTutorAtLeast_WhenTutorHasLowerPlan_ShouldDenyAccess() {
        Usuario alumno = studentUser();
        givenAuthenticatedUser(alumno);
        when(tutorRepository.findByAlumnoId(alumnoId))
                .thenReturn(List.of(tutorWithPlan(TipoPlan.INTERMEDIO)));

        Executable execution =
                () -> guard.requireStudentInheritedTutorAdvanced(authentication());

        assertThrows(AccessDeniedException.class, execution);
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

    private void whenRequiringAtLeast(TipoPlan minimumPlan) {
        guard.requireAtLeast(usuarioId, minimumPlan);
    }

    private void whenRequiringAdvanced() {
        guard.requireAdvanced(usuarioId);
    }

    private void whenRequiringVendorIntermediate() {
        guard.requireVendorIntermediate(authentication());
    }

    private void whenRequiringVendorAdvanced() {
        guard.requireVendorAdvanced(authentication());
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
        Usuario user = user(TipoPlan.GRATUITO, RolUsuario.ALUMNO);
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
