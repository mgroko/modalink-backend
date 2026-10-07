package org.mgroko.backend.repositorio;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.Ciudad;
import org.mgroko.backend.modelo.Pais;
import org.mgroko.backend.modelo.Provincia;
import org.mgroko.backend.modelo.Ubicacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Test de integración de UbicacionRepository contra PostgreSQL real.
 *
 * La clase se anota con @Transactional para que cada método haga rollback y
 * no contamine a los demás tests (la BD es compartida entre todas las
 * clases de integración).
 *
 * <p>El sujeto de prueba es {@code findByCiudad_IdCiudad}, que es la unica via
 * de reuso de una ubicacion. No existe una busqueda por
 * {@code (nombre ciudad, nombre provincia)}: no filtraba por pais ni por
 * fuente, y podia devolver la fila de otra ciudad con el mismo nombre.</p>
 */
@SpringBootTest
@Transactional
class UbicacionRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private UbicacionRepository ubicacionRepository;

    @Autowired
    private CiudadRepository ciudadRepository;

    @Autowired
    private ProvinciaRepository provinciaRepository;

    @Autowired
    private PaisRepository paisRepository;

    private static final String FUENTE = "GEOREF";

    private Pais pais() {
        return paisRepository.findByCodigoIso("AR")
                .orElseGet(() -> paisRepository.save(Pais.builder()
                        .codigoIso("AR")
                        .nombre("Argentina")
                        .activo(true)
                        .build()));
    }

    private Provincia provincia(String nombre) {
        Pais paisArg = pais();
        return provinciaRepository.findByNombreAndPais_IdPais(nombre, paisArg.getIdPais())
                .orElseGet(() -> provinciaRepository.save(Provincia.builder()
                        .nombre(nombre)
                        .idExterno(nombre)
                        .fuenteApi(FUENTE)
                        .activo(true)
                        .pais(paisArg)
                        .build()));
    }

    private Ciudad ciudad(String nombre, String idExterno, String provinciaNombre) {
        Provincia prov = provincia(provinciaNombre);
        return ciudadRepository.findByFuenteApiAndIdExterno(FUENTE, idExterno)
                .orElseGet(() -> ciudadRepository.save(Ciudad.builder()
                        .nombre(nombre)
                        .idExterno(idExterno)
                        .fuenteApi(FUENTE)
                        .activo(true)
                        .provincia(prov)
                        .build()));
    }

    private Ubicacion guardar(String localidad, String provincia, String idExterno) {
        return ubicacionRepository.saveAndFlush(Ubicacion.builder()
                .ciudad(ciudad(localidad, idExterno, provincia))
                .build());
    }

    @Test
    void findByCiudad_IdCiudad_existente_encuentraLaUbicacionDeEsaCadena() {
        Ubicacion guardada = guardar("Saavedra", "Ciudad Autónoma de Buenos Aires", "0208401002");

        Optional<Ubicacion> resultado = ubicacionRepository.findByCiudad_IdCiudad(guardada.getCiudad().getIdCiudad());

        assertTrue(resultado.isPresent());
        assertEquals(guardada.getIdUbicacion(), resultado.get().getIdUbicacion());
        assertEquals("Saavedra", resultado.get().getCiudad().getNombre());
        assertEquals("0208401002", resultado.get().getCiudad().getIdExterno());
        assertEquals("Ciudad Autónoma de Buenos Aires", resultado.get().getCiudad().getProvincia().getNombre());
        assertEquals("Argentina", resultado.get().getCiudad().getProvincia().getPais().getNombre());
    }

    @Test
    void findByCiudad_IdCiudad_inexistente_devuelveVacio() {
        Optional<Ubicacion> resultado = ubicacionRepository.findByCiudad_IdCiudad(999_999L);

        assertTrue(resultado.isEmpty());
    }

    /**
     * El caso que justifico eliminar la busqueda por nombre: dos ciudades con el
     * mismo nombre en provincias distintas son filas distintas y no deben
     * confundirse. Con nombre y provincia como clave, "San Martín" se resolvia
     * contra cualquier provincia; con la clave natural, cada una tiene su fila.
     */
    @Test
    void mismaLocalidadEnOtraProvincia_sonUbicacionesDistintas() {
        Ubicacion caba = guardar("San Martín", "Ciudad Autónoma de Buenos Aires", "0208401006");
        Ubicacion buenosAires = guardar("San Martín", "Buenos Aires", "06442030");

        Optional<Ubicacion> resultado = ubicacionRepository
                .findByCiudad_IdCiudad(buenosAires.getCiudad().getIdCiudad());

        assertTrue(resultado.isPresent());
        assertEquals(buenosAires.getIdUbicacion(), resultado.get().getIdUbicacion());
        assertNotEquals(caba.getIdUbicacion(), resultado.get().getIdUbicacion());
        assertEquals("Buenos Aires", resultado.get().getCiudad().getProvincia().getNombre());
    }
}
