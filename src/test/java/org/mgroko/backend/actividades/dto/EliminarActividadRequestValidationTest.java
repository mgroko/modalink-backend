package org.mgroko.backend.actividades.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.actividades.enums.AccionIntegrante;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class EliminarActividadRequestValidationTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void requestValido_sinDecisiones_noTieneViolaciones() {
        assertThat(validator.validate(
                new EliminarActividadRequest("Falta de presupuesto", List.of())))
                .isEmpty();
    }

    @Test
    void motivoVacio_esInvalido() {
        assertThat(validator.validate(
                new EliminarActividadRequest("", List.of())))
                .extracting(violacion -> violacion.getPropertyPath().toString())
                .contains("motivo");
    }

    @Test
    void decisionesNulas_esInvalido() {
        assertThat(validator.validate(
                new EliminarActividadRequest("Falta de presupuesto", null)))
                .extracting(violacion -> violacion.getPropertyPath().toString())
                .contains("decisiones");
    }

    @Test
    void decisionSinMiembro_esInvalida() {
        assertThat(validator.validate(new EliminarActividadRequest(
                "Falta de presupuesto",
                List.of(new DecisionIntegranteRequest(
                        null, AccionIntegrante.GUARDAR, null)))))
                .extracting(violacion -> violacion.getPropertyPath().toString())
                .contains("decisiones[0].idMiembro");
    }

    @Test
    void decisionSinAccion_esInvalida() {
        assertThat(validator.validate(new EliminarActividadRequest(
                "Falta de presupuesto",
                List.of(new DecisionIntegranteRequest(
                        20L, null, null)))))
                .extracting(violacion -> violacion.getPropertyPath().toString())
                .contains("decisiones[0].accion");
    }

    @Test
    void motivoBajaDe101Caracteres_esInvalido() {
        assertThat(validator.validate(new EliminarActividadRequest(
                "Falta de presupuesto",
                List.of(new DecisionIntegranteRequest(
                        20L, AccionIntegrante.ELIMINAR, "x".repeat(101))))))
                .extracting(violacion -> violacion.getPropertyPath().toString())
                .contains("decisiones[0].motivoBaja");
    }
}
