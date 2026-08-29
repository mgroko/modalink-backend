package org.mgroko.backend.repositorio;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.CaracteristicaTecnica;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test de integración de la búsqueda de características técnicas (UC-58)
 * contra PostgreSQL real. V8/V10 dejan sembradas las 8 características de la
 * profesión "modelo"; este test valida la query de
 * {@link CaracteristicaTecnicaRepository#buscar}.
 */
@SpringBootTest
class CaracteristicaTecnicaRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private CaracteristicaTecnicaRepository caracteristicaTecnicaRepository;

    @Autowired
    private ProfesionRepository profesionRepository;

    private Long idProfesionModelo() {
        return profesionRepository.buscar("%modelo%").get(0).getIdProfesion();
    }

    @Test
    void buscar_sinFiltro_devuelveTodasLasSembradas() {
        List<CaracteristicaTecnica> resultado = caracteristicaTecnicaRepository.buscar("%", "%", null);

        assertEquals(8, resultado.size());
    }

    @Test
    void buscar_porProfesion_devuelveLasDeModelo() {
        List<CaracteristicaTecnica> resultado =
                caracteristicaTecnicaRepository.buscar("%", "%", idProfesionModelo());

        assertEquals(8, resultado.size());
        assertTrue(resultado.stream().allMatch(c -> c.getProfesion().getIdProfesion().equals(idProfesionModelo())));
    }

    @Test
    void buscar_porCodigo_devuelveCoincidencia() {
        List<CaracteristicaTecnica> resultado = caracteristicaTecnicaRepository.buscar("%altura%", "%", null);

        assertEquals(1, resultado.size());
        assertEquals("altura", resultado.get(0).getCodigo());
    }

    @Test
    void buscar_porUnidad_color_devuelveTres() {
        List<CaracteristicaTecnica> resultado = caracteristicaTecnicaRepository.buscar("%", "%color%", null);

        assertEquals(3, resultado.size());
    }

    @Test
    void buscar_combinandoFiltros_devuelveCoincidencia() {
        List<CaracteristicaTecnica> resultado =
                caracteristicaTecnicaRepository.buscar("%medida_pecho%", "%", idProfesionModelo());

        assertEquals(1, resultado.size());
        assertEquals("medida_pecho", resultado.get(0).getCodigo());
    }

    @Test
    void buscar_sinCoincidencias_devuelveVacio() {
        List<CaracteristicaTecnica> resultado =
                caracteristicaTecnicaRepository.buscar("%inexistente%", "%", idProfesionModelo());

        assertTrue(resultado.isEmpty());
    }
}