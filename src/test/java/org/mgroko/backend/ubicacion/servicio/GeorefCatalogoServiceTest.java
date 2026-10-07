package org.mgroko.backend.ubicacion.servicio;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.ubicacion.catalogo.FuenteCatalogo;
import org.mgroko.backend.ubicacion.catalogo.LocalidadCatalogo;
import org.mgroko.backend.ubicacion.catalogo.ProvinciaCatalogo;
import org.mgroko.backend.ubicacion.exception.LocalidadNoEncontradaException;
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
        List<ProvinciaCatalogo> provincias = servicio.listarProvincias();

        assertEquals(24, provincias.size());
        assertTrue(provincias.stream().anyMatch(p -> p.nombre().equals("Ciudad Autónoma de Buenos Aires")));
        assertTrue(provincias.stream().anyMatch(p -> p.nombre().equals("Buenos Aires")));
        for (int i = 1; i < provincias.size(); i++) {
            assertTrue(provincias.get(i - 1).nombre().compareTo(provincias.get(i).nombre()) <= 0);
        }
    }

    @Test
    void listarProvincias_exponeIdYFuenteDelDominio() {
        ProvinciaCatalogo caba = servicio.listarProvincias().stream()
                .filter(p -> p.nombre().equals("Ciudad Autónoma de Buenos Aires"))
                .findFirst()
                .orElseThrow();

        assertEquals("02", caba.idExterno());
        assertEquals(FuenteCatalogo.GEOREF, caba.fuente());
    }

    @Test
    void buscarLocalidades_sinFiltros_devuelveTodas() {
        List<LocalidadCatalogo> localidades = servicio.buscarLocalidades(null, null);

        assertEquals(4037, localidades.size());
    }

    @Test
    void buscarLocalidades_porProvincia_filtra() {
        List<LocalidadCatalogo> localidades = servicio.buscarLocalidades("02", null);

        assertTrue(localidades.stream().allMatch(l -> l.idProvincia().equals("02")));
        assertEquals("Ciudad Autónoma de Buenos Aires",
                localidades.get(0).nombreProvincia());
        assertTrue(localidades.stream().anyMatch(l -> l.nombre().equals("Saavedra")));
    }

    @Test
    void buscarLocalidades_porNombre_filtraSinDistinguirMayusculas() {
        List<LocalidadCatalogo> localidades = servicio.buscarLocalidades(null, "LA PLATA");

        assertTrue(localidades.stream().allMatch(l -> l.nombre().toLowerCase().contains("la plata")));
        assertTrue(localidades.stream().anyMatch(l -> l.nombre().equals("La Plata")));
    }

    @Test
    void buscarLocalidades_combinandoFiltros_devuelveCoincidencia() {
        List<LocalidadCatalogo> localidades = servicio.buscarLocalidades("82", "constituci");

        assertEquals(2, localidades.size());
        assertTrue(localidades.stream()
                .allMatch(l -> l.idProvincia().equals("82")
                        && l.nombre().toLowerCase().contains("constituci")));
    }

    @Test
    void buscarLocalidades_sinCoincidencias_devuelveVacio() {
        assertTrue(servicio.buscarLocalidades("99", "inexistente").isEmpty());
    }

    @Test
    void localidadesIncluyenCentroide() {
        List<LocalidadCatalogo> localidades = servicio.buscarLocalidades("02", null);

        LocalidadCatalogo saavedra = localidades.stream()
                .filter(l -> l.nombre().equals("Saavedra"))
                .findFirst()
                .orElseThrow();

        assertNotNull(saavedra.latitud());
        assertNotNull(saavedra.longitud());
    }

    @Test
    void obtenerLocalidad_existente_devuelveDatos() {
        LocalidadCatalogo localidad = servicio.obtenerLocalidad("0208401002");

        assertEquals("Saavedra", localidad.nombre());
        assertEquals("02", localidad.idProvincia());
        assertEquals("Ciudad Autónoma de Buenos Aires", localidad.nombreProvincia());
        assertEquals(FuenteCatalogo.GEOREF, localidad.fuente());
        assertTrue(localidad.latitud().compareTo(BigDecimal.ZERO) < 0);
    }

    @Test
    void obtenerLocalidad_inexistente_lanzaExcepcion() {
        assertThrows(LocalidadNoEncontradaException.class,
                () -> servicio.obtenerLocalidad("9999999999"));
    }
}
