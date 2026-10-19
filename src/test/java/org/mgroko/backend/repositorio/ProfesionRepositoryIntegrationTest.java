package org.mgroko.backend.repositorio;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Profesion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ProfesionRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private ProfesionRepository profesionRepository;

    @Test
    void buscar_sinFiltro_devuelveTodasLasSembradas() {
        List<Profesion> resultado = profesionRepository.buscar("%", "");

        assertEquals(7, resultado.size());
    }

    @Test
    void buscar_porNombre_devuelveCoincidencias() {
        List<Profesion> resultado = profesionRepository.buscar("%modelo%", "modelo");

        assertEquals(1, resultado.size());
        assertEquals("Modelo", resultado.get(0).getNombre());
    }

    @Test
    void buscar_esCaseInsensitive() {
        List<Profesion> resultado = profesionRepository.buscar("%MODELO%".toLowerCase(), "MODELO");

        assertEquals(1, resultado.size());
        assertEquals("Modelo", resultado.get(0).getNombre());
    }

    @Test
    void buscar_porCodigoExacto_devuelveCoincidencia() {
        List<Profesion> resultado = profesionRepository.buscar("%diseniador_moda%", "DISENIADOR_MODA");

        assertEquals(1, resultado.size());
        assertEquals("DISENIADOR_MODA", resultado.get(0).getCodigo());
        assertEquals("Diseñador de Moda", resultado.get(0).getNombre());
    }

    @Test
    void buscar_porCodigoEsCaseInsensitive() {
        List<Profesion> resultado = profesionRepository.buscar("%diseniador_moda%", "diseniador_moda");

        assertEquals(1, resultado.size());
        assertEquals("DISENIADOR_MODA", resultado.get(0).getCodigo());
    }

    @Test
    void buscar_porCodigoParcial_noDevuelveCoincidencia() {
        List<Profesion> resultado = profesionRepository.buscar("%diseniador%", "DISENIADOR");

        assertTrue(resultado.isEmpty());
    }

    @Test
    void buscar_queContengaTexto_devuelveCoincidencias() {
        List<Profesion> resultado = profesionRepository.buscar("%estilista%", "estilista");

        assertEquals(2, resultado.size());
    }

    @Test
    void buscar_sinCoincidencias_devuelveVacio() {
        List<Profesion> resultado = profesionRepository.buscar("%profesion_inexistente%", "profesion_inexistente");

        assertTrue(resultado.isEmpty());
    }
}