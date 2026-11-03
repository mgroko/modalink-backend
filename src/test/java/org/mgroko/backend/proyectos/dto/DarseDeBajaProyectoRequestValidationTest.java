package org.mgroko.backend.proyectos.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class DarseDeBajaProyectoRequestValidationTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void motivoValido_noTieneErrores() {
        assertThat(validator.validate(
                new DarseDeBajaProyectoRequest("Nuevo proyecto"))).isEmpty();
    }

    @Test
    void motivoVacio_tieneError() {
        assertThat(validator.validate(new DarseDeBajaProyectoRequest("")))
                .isNotEmpty();
    }

    @Test
    void motivoSoloEspacios_tieneError() {
        assertThat(validator.validate(new DarseDeBajaProyectoRequest("   ")))
                .isNotEmpty();
    }

    @Test
    void motivoNulo_tieneError() {
        assertThat(validator.validate(new DarseDeBajaProyectoRequest(null)))
                .isNotEmpty();
    }

    @Test
    void motivoDe100Caracteres_esValido() {
        assertThat(validator.validate(
                new DarseDeBajaProyectoRequest("x".repeat(100)))).isEmpty();
    }

    @Test
    void motivoDe101Caracteres_tieneError() {
        assertThat(validator.validate(
                new DarseDeBajaProyectoRequest("x".repeat(101)))).isNotEmpty();
    }
}
