package org.mgroko.backend.ubicacion.dto;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class UbicacionRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void localidadIdValido_sinViolaciones() {
        UbicacionRequest request = new UbicacionRequest("0208401002");

        Set<ConstraintViolation<UbicacionRequest>> violaciones = validator.validate(request);

        assertTrue(violaciones.isEmpty());
    }

    @Test
    void localidadIdNulo_generaViolacion() {
        UbicacionRequest request = new UbicacionRequest(null);

        Set<ConstraintViolation<UbicacionRequest>> violaciones = validator.validate(request);

        assertEquals(1, violaciones.size());
        assertTrue(violaciones.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("localidadId")));
    }

    @Test
    void localidadIdVacio_generaViolacion() {
        UbicacionRequest request = new UbicacionRequest("");

        Set<ConstraintViolation<UbicacionRequest>> violaciones = validator.validate(request);

        assertEquals(1, violaciones.size());
        assertTrue(violaciones.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("localidadId")));
    }

    @Test
    void localidadIdEnBlanco_generaViolacion() {
        UbicacionRequest request = new UbicacionRequest("   ");

        Set<ConstraintViolation<UbicacionRequest>> violaciones = validator.validate(request);

        assertEquals(1, violaciones.size());
        assertTrue(violaciones.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("localidadId")));
    }
}