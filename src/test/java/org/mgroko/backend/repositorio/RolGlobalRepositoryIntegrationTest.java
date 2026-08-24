package org.mgroko.backend.repositorio;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mgroko.backend.modelo.RolGlobal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test de integración de RolGlobalRepository contra PostgreSQL real.
 *
 * El contenedor se comparte vía {@link AbstractPostgresIntegrationTest};
 * valida que findByNombre funcione contra los roles sembrados en
 * V1__esquema_base_modalink.sql ("Administrador", "Usuario").
 *
 * Solo lee datos sembrados: no necesita @Transactional ni limpieza.
 */
@SpringBootTest
class RolGlobalRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private RolGlobalRepository rolGlobalRepository;

    @ParameterizedTest
    @ValueSource(strings = { "Administrador", "Usuario" })
    void findByNombre_rolesSembrados_encontrados(String nombre) {
        Optional<RolGlobal> resultado = rolGlobalRepository.findByNombre(nombre);

        assertTrue(resultado.isPresent());
        assertEquals(nombre, resultado.get().getNombre());
    }

    @Test
    void findByNombre_nombreInexistente_devuelveVacio() {
        Optional<RolGlobal> resultado = rolGlobalRepository.findByNombre("rol_inventado");

        assertTrue(resultado.isEmpty());
    }

    @Test
    void findByNombre_esCaseSensitive() {
        Optional<RolGlobal> resultado = rolGlobalRepository.findByNombre("usuario");

        assertTrue(resultado.isEmpty());
    }
}
