package org.mgroko.backend.repositorio;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mgroko.backend.modelo.Genero;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test de integración de GeneroRepository contra PostgreSQL real.
 *
 * El contenedor se comparte vía {@link AbstractPostgresIntegrationTest};
 * Flyway ejecuta las migraciones (V1 a V3) y este test valida que
 * GeneroRepository.findByCodigo funcione contra los códigos sembrados en
 * V3__adicion_de_genero.sql. Al usar ddl-auto=validate, también verifica
 * que las entidades mapeadas coinciden con el esquema migrado.
 *
 * Solo lee datos sembrados: no necesita @Transactional ni limpieza.
 */
@SpringBootTest
class GeneroRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private GeneroRepository generoRepository;

    @ParameterizedTest
    @ValueSource(strings = { "mujer", "hombre", "no_binario", "no_decirlo" })
    void findByCodigo_codigosSembrados_encontrados(String codigo) {
        Optional<Genero> resultado = generoRepository.findByCodigo(codigo);

        assertTrue(resultado.isPresent());
        assertEquals(codigo, resultado.get().getCodigo());
    }

    @Test
    void findByCodigo_codigoInexistente_devuelveVacio() {
        Optional<Genero> resultado = generoRepository.findByCodigo("genero_inventado");

        assertTrue(resultado.isEmpty());
    }

    @Test
    void findByCodigo_esCaseSensitive() {

        Optional<Genero> resultado = generoRepository.findByCodigo("MUJER");

        assertTrue(resultado.isEmpty());
    }
}
