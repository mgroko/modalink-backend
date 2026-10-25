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

@SpringBootTest
class GeneroRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private GeneroRepository generoRepository;

    @ParameterizedTest
    @ValueSource(strings = { "mujer", "hombre", "no_binario", "no_decirlo" })
    void findByCodigo_codigosSembrados_encontrados(String codigo) {
        Optional<Genero> resultado = generoRepository.findByCodigo(codigo);

        assertTrue(resultado.isPresent());
        assertEquals(codigo.toUpperCase(), resultado.get().getCodigo());
    }

    @Test
    void findByCodigo_codigoInexistente_devuelveVacio() {
        Optional<Genero> resultado = generoRepository.findByCodigo("genero_inventado");

        assertTrue(resultado.isEmpty());
    }

    @Test
    void findByCodigo_esCaseInsensitive() {

        Optional<Genero> resultado = generoRepository.findByCodigo("MUJER");

        assertTrue(resultado.isPresent());
        assertEquals("MUJER", resultado.get().getCodigo());
    }
}
