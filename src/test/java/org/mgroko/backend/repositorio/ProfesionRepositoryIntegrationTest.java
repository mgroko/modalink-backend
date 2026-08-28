package org.mgroko.backend.repositorio;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Profesion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test de integración de la búsqueda de profesiones (UC-59) contra PostgreSQL
 * real. Flyway ejecuta las migraciones y V5/V6 dejan sembradas las profesiones
 * base; este test valida la query de {@link ProfesionRepository#buscar}.
 */
@SpringBootTest
class ProfesionRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private ProfesionRepository profesionRepository;

    @Test
    void buscar_sinFiltro_devuelveTodasLasSembradas() {
        List<Profesion> resultado = profesionRepository.buscar("%");

        assertEquals(7, resultado.size());
    }

    @Test
    void buscar_porNombre_devuelveCoincidencias() {
        List<Profesion> resultado = profesionRepository.buscar("%modelo%");

        assertEquals(1, resultado.size());
        assertEquals("modelo", resultado.get(0).getNombre());
    }

    @Test
    void buscar_esCaseInsensitive() {
        List<Profesion> resultado = profesionRepository.buscar("%MODELO%".toLowerCase());

        assertEquals(1, resultado.size());
        assertEquals("modelo", resultado.get(0).getNombre());
    }

    @Test
    void buscar_queContengaTexto_devuelveCoincidencias() {
        List<Profesion> resultado = profesionRepository.buscar("%estilista%");

        assertEquals(2, resultado.size());
    }

    @Test
    void buscar_sinCoincidencias_devuelveVacio() {
        List<Profesion> resultado = profesionRepository.buscar("%profesion_inexistente%");

        assertTrue(resultado.isEmpty());
    }
}