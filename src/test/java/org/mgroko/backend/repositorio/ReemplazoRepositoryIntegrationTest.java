package org.mgroko.backend.repositorio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.repositorio.AbstractPostgresIntegrationTest;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class ReemplazoRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private PerfilRepository perfilRepository;

    @Test
    @Transactional
    void consultaCandidatosDeActividadEjecutaSinErrores() {
        assertThat(perfilRepository.buscarCandidatosReemplazoActividad(
                999L, 999L, 999L)).isEmpty();
    }

    @Test
    @Transactional
    void consultaCandidatosGeneralesEjecutaSinErrores() {
        assertThat(perfilRepository.buscarCandidatosReemplazoGeneral(
                999L, 999L)).isEmpty();
    }
}
