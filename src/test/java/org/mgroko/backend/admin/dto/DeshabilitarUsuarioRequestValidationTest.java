package org.mgroko.backend.admin.dto;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class DeshabilitarUsuarioRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private DeshabilitarUsuarioRequest request(String motivo, Integer duracionDias) {
        return new DeshabilitarUsuarioRequest(motivo, duracionDias);
    }

    @Test
    void valido_conDuracion_sinViolaciones() {
        assertTrue(validator.validate(request("Incumplimiento de normas", 7)).isEmpty());
    }

    @Test
    void valido_sinDuracion_sinViolaciones() {
        assertTrue(validator.validate(request("Motivo", null)).isEmpty());
    }

    @Test
    void motivoNulo_generaViolacion() {
        Set<ConstraintViolation<DeshabilitarUsuarioRequest>> violaciones =
                validator.validate(request(null, 7));

        assertTrue(violaciones.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("motivo")));
    }

    @Test
    void motivoVacio_generaViolacion() {
        Set<ConstraintViolation<DeshabilitarUsuarioRequest>> violaciones =
                validator.validate(request("", 7));

        assertTrue(violaciones.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("motivo")));
    }

    @Test
    void motivoMayorA200_generaViolacion() {
        Set<ConstraintViolation<DeshabilitarUsuarioRequest>> violaciones =
                validator.validate(request("a".repeat(201), 7));

        assertTrue(violaciones.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("motivo")));
    }

    @Test
    void duracionCero_generaViolacion() {
        Set<ConstraintViolation<DeshabilitarUsuarioRequest>> violaciones =
                validator.validate(request("Motivo", 0));

        assertTrue(violaciones.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("duracionDias")));
    }

    @Test
    void duracionNegativa_generaViolacion() {
        Set<ConstraintViolation<DeshabilitarUsuarioRequest>> violaciones =
                validator.validate(request("Motivo", -1));

        assertTrue(violaciones.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("duracionDias")));
    }
}