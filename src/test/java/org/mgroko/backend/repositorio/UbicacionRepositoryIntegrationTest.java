package org.mgroko.backend.repositorio;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
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
 */
@SpringBootTest
@Transactional
class UbicacionRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private UbicacionRepository ubicacionRepository;

    private Ubicacion guardar(String localidad, String provincia) {
        return guardar(localidad, provincia, "X");
    }

    private Ubicacion guardar(String localidad, String provincia, String idGeoref) {
        return ubicacionRepository.saveAndFlush(Ubicacion.builder()
                .localidad(localidad)
                .provincia(provincia)
                .idGeoref(idGeoref)
                .build());
    }

    @Test
    void findByLocalidadAndProvincia_existente_encontrada() {
        guardar("Saavedra", "Ciudad Autónoma de Buenos Aires", "0208401002");

        Optional<Ubicacion> resultado = ubicacionRepository
                .findByLocalidadAndProvincia("Saavedra", "Ciudad Autónoma de Buenos Aires");

        assertTrue(resultado.isPresent());
        assertEquals("Saavedra", resultado.get().getLocalidad());
        assertEquals("Ciudad Autónoma de Buenos Aires", resultado.get().getProvincia());
        assertEquals("0208401002", resultado.get().getIdGeoref());
    }

    @Test
    void findByLocalidadAndProvincia_inexistente_devuelveVacio() {
        Optional<Ubicacion> resultado = ubicacionRepository
                .findByLocalidadAndProvincia("Localidad Inexistente", "Provincia Inexistente");

        assertTrue(resultado.isEmpty());
    }

    @Test
    void findByLocalidadAndProvincia_mismaLocalidadEnOtraProvincia_esDistinta() {
        Ubicacion caba = guardar("San Martín", "Ciudad Autónoma de Buenos Aires");
        Ubicacion buenosAires = guardar("San Martín", "Buenos Aires");

        Optional<Ubicacion> resultado = ubicacionRepository
                .findByLocalidadAndProvincia("San Martín", "Buenos Aires");

        assertTrue(resultado.isPresent());
        assertEquals(buenosAires.getIdUbicacion(), resultado.get().getIdUbicacion());
        assertTrue(!resultado.get().getIdUbicacion().equals(caba.getIdUbicacion()));
    }
}