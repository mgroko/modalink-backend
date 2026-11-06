package org.mgroko.backend.proyectos.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class EliminarIntegranteRequestValidationTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void motivoValido_noTieneViolaciones() {
        assertThat(validator.validate(
                new EliminarIntegranteRequest("Falta de compromiso"))).isEmpty();
    }

    @Test
    void motivoVacio_esInvalido() {
        assertThat(validator.validate(new EliminarIntegranteRequest("")))
                .extracting(violacion -> violacion.getPropertyPath().toString())
                .contains("motivo");
    }

    @Test
    void motivoSoloEspacios_esInvalido() {
        assertThat(validator.validate(new EliminarIntegranteRequest("   ")))
                .extracting(violacion -> violacion.getPropertyPath().toString())
                .contains("motivo");
    }

    @Test
    void motivoNulo_esInvalido() {
        assertThat(validator.validate(new EliminarIntegranteRequest(null)))
                .extracting(violacion -> violacion.getPropertyPath().toString())
                .contains("motivo");
    }

    @Test
    void motivoDe100Caracteres_esValido() {
        assertThat(validator.validate(
                new EliminarIntegranteRequest("x".repeat(100)))).isEmpty();
    }

    @Test
    void motivoDe101Caracteres_esInvalido() {
        assertThat(validator.validate(
                new EliminarIntegranteRequest("x".repeat(101))))
                .extracting(violacion -> violacion.getPropertyPath().toString())
                .contains("motivo");
    }
}
