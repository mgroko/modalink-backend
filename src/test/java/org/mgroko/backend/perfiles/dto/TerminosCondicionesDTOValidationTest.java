package org.mgroko.backend.perfiles.dto;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class TerminosCondicionesDTOValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void actualizar_textoValido_sinViolaciones() {
        assertTrue(validator.validate(
                new ActualizarTerminosCondicionesRequest("Texto de condiciones")).isEmpty());
    }

    @Test
    void actualizar_textoVacio_generaViolacion() {
        Set<ConstraintViolation<ActualizarTerminosCondicionesRequest>> violaciones =
                validator.validate(new ActualizarTerminosCondicionesRequest("   "));
        assertTrue(violaciones.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("descripcion")));
    }

    @Test
    void actualizar_textoMaximo_sinViolaciones() {
        assertTrue(validator.validate(
                new ActualizarTerminosCondicionesRequest("x".repeat(5000))).isEmpty());
    }

    @Test
    void actualizar_textoExcedido_generaViolacion() {
        Set<ConstraintViolation<ActualizarTerminosCondicionesRequest>> violaciones =
                validator.validate(new ActualizarTerminosCondicionesRequest("x".repeat(5001)));
        assertTrue(violaciones.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("descripcion")));
    }

    @Test
    void aceptacion_valida_sinViolaciones() {
        assertTrue(validator.validate(
                new AceptacionTerminosRequest(7L, true)).isEmpty());
    }

    @Test
    void aceptacion_sinIdTyc_generaViolacion() {
        Set<ConstraintViolation<AceptacionTerminosRequest>> violaciones =
                validator.validate(new AceptacionTerminosRequest(null, true));
        assertTrue(violaciones.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("idTyc")));
    }

    @Test
    void aceptacion_sinConfirmacion_generaViolacion() {
        Set<ConstraintViolation<AceptacionTerminosRequest>> violaciones =
                validator.validate(new AceptacionTerminosRequest(7L, null));
        assertTrue(violaciones.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("aceptaTerminos")));
    }
}
