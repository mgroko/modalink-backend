package org.mgroko.backend.proyectos.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class CalificarEquipoRequestValidationTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void requestValido_noTieneViolaciones() {
        assertThat(validator.validate(new CalificarEquipoRequest(
                List.of(new CalificacionRequest(20L, 5, "Excelente desempeño")))))
                .isEmpty();
    }

    @Test
    void listaNula_esInvalida() {
        assertThat(validator.validate(new CalificarEquipoRequest(null)))
                .extracting(violacion -> violacion.getPropertyPath().toString())
                .contains("calificaciones");
    }

    @Test
    void calificacionAnidadaInvalida_esDetectada() {
        assertThat(validator.validate(new CalificarEquipoRequest(
                List.of(new CalificacionRequest(20L, 6, "Excelente")))))
                .extracting(violacion -> violacion.getPropertyPath().toString())
                .contains("calificaciones[0].valoracion");
    }
}
