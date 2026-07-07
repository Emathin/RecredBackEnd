package com.recred.backend.unit.config;

import com.recred.backend.config.DefaultSchoolLevelsInitializer;
import com.recred.backend.domain.models.Nivel;
import com.recred.backend.domain.repositories.NivelRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultSchoolLevelsInitializerTest {

    private final NivelRepository repository = mock(NivelRepository.class);
    private final DefaultSchoolLevelsInitializer initializer =
            new DefaultSchoolLevelsInitializer(repository);

    @Test
    void run_WhenLevelsDoNotExist_ShouldCreateDefaultLevels() {
        givenNoDefaultLevelsExist();

        whenRunningInitializer();

        verify(repository).save(argThat(level -> "Inicial".equals(level.getDescripcion())));
        verify(repository).save(argThat(level -> "Primario".equals(level.getDescripcion())));
        verify(repository).save(argThat(level -> "Secundario".equals(level.getDescripcion())));
        verify(repository, times(3)).save(argThat(Nivel::getActivo));
    }

    @Test
    void run_WhenLevelAlreadyExists_ShouldNotCreateItAgain() {
        givenExistingLevel("Primario");

        whenRunningInitializer();

        verify(repository, never())
                .save(argThat(level -> "Primario".equals(level.getDescripcion())));
    }

    @Test
    void run_WhenDefaultLevelExistsInactive_ShouldActivateIt() {
        givenExistingInactiveLevel("Primario");

        whenRunningInitializer();

        verify(repository).save(argThat(level ->
                "Primario".equals(level.getDescripcion()) && Boolean.TRUE.equals(level.getActivo())));
    }

    private void givenNoDefaultLevelsExist() {
        when(repository.findByDescripcionIgnoreCase("Inicial")).thenReturn(Optional.empty());
        when(repository.findByDescripcionIgnoreCase("Primario")).thenReturn(Optional.empty());
        when(repository.findByDescripcionIgnoreCase("Secundario")).thenReturn(Optional.empty());
    }

    private void givenExistingLevel(String descripcion) {
        when(repository.findByDescripcionIgnoreCase("Inicial")).thenReturn(Optional.empty());
        when(repository.findByDescripcionIgnoreCase(descripcion))
                .thenReturn(Optional.of(level(descripcion)));
        when(repository.findByDescripcionIgnoreCase("Secundario")).thenReturn(Optional.empty());
    }

    private void givenExistingInactiveLevel(String descripcion) {
        when(repository.findByDescripcionIgnoreCase("Inicial")).thenReturn(Optional.empty());
        when(repository.findByDescripcionIgnoreCase(descripcion))
                .thenReturn(Optional.of(inactiveLevel(descripcion)));
        when(repository.findByDescripcionIgnoreCase("Secundario")).thenReturn(Optional.empty());
    }

    private void whenRunningInitializer() {
        initializer.run(null);
    }

    private Nivel level(String descripcion) {
        Nivel level = new Nivel();
        level.setDescripcion(descripcion);
        level.setActivo(true);
        return level;
    }

    private Nivel inactiveLevel(String descripcion) {
        Nivel level = new Nivel();
        level.setDescripcion(descripcion);
        level.setActivo(false);
        return level;
    }
}
