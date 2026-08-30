package org.mgroko.backend.ubicacion.servicio;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.ubicacion.dto.LocalidadResponse;
import org.mgroko.backend.ubicacion.dto.ProvinciaResponse;
import org.mgroko.backend.ubicacion.exception.LocalidadNoEncontradaException;
import org.mgroko.backend.ubicacion.georef.LocalidadGeoref;
import org.springframework.core.io.DefaultResourceLoader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

/**
 * Test del catálogo Georef contra los archivos reales versionados en
 * {@code src/main/resources/georef/}.
 */
class GeorefCatalogoServiceTest {

    private static GeorefCatalogoService servicio;

    @BeforeAll
    static void cargarCatalogoReal() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new ParameterNamesModule());
        servicio = new GeorefCatalogoService(objectMapper, new DefaultResourceLoader());
    }

    @Test
    void listarProvincias_devuelveLas24Ordenadas() {
        List<ProvinciaResponse> provincias = servicio.listarProvincias();

        assertEquals(24, provincias.size());
        assertTrue(provincias.stream().anyMatch(p -> p.nombre().equals("Ciudad Autónoma de Buenos Aires")));
        assertTrue(provincias.stream().anyMatch(p -> p.nombre().equals("Buenos Aires")));
        for (int i = 1; i < provincias.size(); i++) {
            assertTrue(provincias.get(i - 1).nombre().compareTo(provincias.get(i).nombre()) <= 0);
        }
    }

    @Test
    void buscarLocalidades_sinFiltros_devuelveTodas() {
        List<LocalidadResponse> localidades = servicio.buscarLocalidades(null, null);

        assertEquals(4037, localidades.size());
    }

    @Test
    void buscarLocalidades_porProvincia_filtra() {
        List<LocalidadResponse> localidades = servicio.buscarLocalidades("02", null);

        assertTrue(localidades.stream().allMatch(l -> l.provinciaId().equals("02")));
        assertEquals("Ciudad Autónoma de Buenos Aires",
                localidades.get(0).provinciaNombre());
        assertTrue(localidades.stream().anyMatch(l -> l.nombre().equals("Saavedra")));
    }

    @Test
    void buscarLocalidades_porNombre_filtraSinDistinguirMayusculas() {
        List<LocalidadResponse> localidades = servicio.buscarLocalidades(null, "LA PLATA");

        assertTrue(localidades.stream().allMatch(l -> l.nombre().toLowerCase().contains("la plata")));
        assertTrue(localidades.stream().anyMatch(l -> l.nombre().equals("La Plata")));
    }

    @Test
    void buscarLocalidades_combinandoFiltros_devuelveCoincidencia() {
        List<LocalidadResponse> localidades = servicio.buscarLocalidades("82", "constituci");

        assertEquals(2, localidades.size());
        assertTrue(localidades.stream()
                .allMatch(l -> l.provinciaId().equals("82")
                        && l.nombre().toLowerCase().contains("constituci")));
    }

    @Test
    void buscarLocalidades_sinCoincidencias_devuelveVacio() {
        assertTrue(servicio.buscarLocalidades("99", "inexistente").isEmpty());
    }

    @Test
    void localidadesIncluyenCentroide() {
        List<LocalidadResponse> localidades = servicio.buscarLocalidades("02", null);

        LocalidadResponse saavedra = localidades.stream()
                .filter(l -> l.nombre().equals("Saavedra"))
                .findFirst()
                .orElseThrow();

        assertNotNull(saavedra.latitud());
        assertNotNull(saavedra.longitud());
    }

    @Test
    void obtenerLocalidad_existente_devuelveDatos() {
        LocalidadGeoref localidad = servicio.obtenerLocalidad("0208401002");

        assertEquals("Saavedra", localidad.nombre());
        assertEquals("02", localidad.provincia().id());
        assertTrue(localidad.centroide().lat() < 0);
    }

    @Test
    void obtenerLocalidad_inexistente_lanzaExcepcion() {
        assertThrows(LocalidadNoEncontradaException.class,
                () -> servicio.obtenerLocalidad("9999999999"));
    }
}