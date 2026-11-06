package org.mgroko.backend.repositorio;

import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Habilidad;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class HabilidadRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private HabilidadRepository habilidadRepository;

    @Test
    void buscar_sinFiltro_incluyeLosSeeds() {
        List<Habilidad> resultado = habilidadRepository.buscar("%");

        assertTrue(resultado.size() >= 10);
        assertTrue(resultado.stream()
                .anyMatch(h -> h.getNombre().equals("Fotografía de Estudio")));
    }

    @Test
    void buscar_porNombre_esCaseInsensitive() {
        List<Habilidad> resultado = habilidadRepository.buscar("%fotograf%");

        assertEquals(2, resultado.size());
        assertTrue(resultado.stream()
                .allMatch(h -> h.getNombre().toLowerCase().contains("fotograf")));
    }

    @Test
    void buscar_porNombreParcial_devuelveCoincidencia() {
        List<Habilidad> resultado = habilidadRepository.buscar("%modelaje%");

        assertEquals(1, resultado.size());
        assertEquals("Modelaje Editorial", resultado.get(0).getNombre());
    }

    @Test
    void buscar_sinCoincidencias_devuelveListaVacia() {
        assertTrue(habilidadRepository.buscar("%inexistente%").isEmpty());
    }

    @Test
    void buscar_ordenaPorNombre() {
        List<Habilidad> resultado = habilidadRepository.buscar("%");

        List<String> nombres = resultado.stream().map(Habilidad::getNombre).toList();
        List<String> ordenados = nombres.stream()
                .sorted(Comparator.naturalOrder())
                .toList();

        assertEquals(ordenados, nombres);
    }

    @Test
    void existsByNombreIgnoreCase_encuentraSeed() {
        assertTrue(habilidadRepository.existsByNombreIgnoreCase("fotografía de estudio"));
    }

    @Test
    void existsByCodigoIgnoreCase_encuentraSeed() {
        assertTrue(habilidadRepository.existsByCodigoIgnoreCase("foto_estudio"));
    }

    @Test
    void guardar_generaIdentificador() {
        Habilidad habilidad = Habilidad.builder()
                .codigo("TEST_INTEGRACION")
                .nombre("Habilidad Test Integración")
                .descripcion("Creada por el test de integración.")
                .build();

        Habilidad guardada = habilidadRepository.saveAndFlush(habilidad);

        assertNotNull(guardada.getIdHabilidad());
    }
}
