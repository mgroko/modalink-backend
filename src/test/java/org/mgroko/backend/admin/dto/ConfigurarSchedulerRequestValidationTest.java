package org.mgroko.backend.admin.dto;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class ConfigurarSchedulerRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void requestValido_sinViolaciones() {
        ConfigurarSchedulerRequest request = new ConfigurarSchedulerRequest(3, 0);

        Set<ConstraintViolation<ConfigurarSchedulerRequest>> violaciones =
                validator.validate(request);

        assertTrue(violaciones.isEmpty());
    }

    @Test
    void horaNula_generaViolacion() {
        ConfigurarSchedulerRequest request = new ConfigurarSchedulerRequest(null, 15);

        Set<ConstraintViolation<ConfigurarSchedulerRequest>> violaciones =
                validator.validate(request);

        assertFalse(violaciones.isEmpty());
        assertTrue(violaciones.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("hora")));
    }

    @Test
    void horaNegativa_generaViolacion() {
        ConfigurarSchedulerRequest request = new ConfigurarSchedulerRequest(-1, 0);

        Set<ConstraintViolation<ConfigurarSchedulerRequest>> violaciones =
                validator.validate(request);

        assertFalse(violaciones.isEmpty());
        assertTrue(violaciones.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("hora")));
    }

    @Test
    void horaMayorA23_generaViolacion() {
        ConfigurarSchedulerRequest request = new ConfigurarSchedulerRequest(24, 0);

        Set<ConstraintViolation<ConfigurarSchedulerRequest>> violaciones =
                validator.validate(request);

        assertFalse(violaciones.isEmpty());
        assertTrue(violaciones.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("hora")));
    }

    @Test
    void minutoNulo_generaViolacion() {
        ConfigurarSchedulerRequest request = new ConfigurarSchedulerRequest(12, null);

        Set<ConstraintViolation<ConfigurarSchedulerRequest>> violaciones =
                validator.validate(request);

        assertFalse(violaciones.isEmpty());
        assertTrue(violaciones.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("minuto")));
    }

    @Test
    void minutoNegativo_generaViolacion() {
        ConfigurarSchedulerRequest request = new ConfigurarSchedulerRequest(12, -1);

        Set<ConstraintViolation<ConfigurarSchedulerRequest>> violaciones =
                validator.validate(request);

        assertFalse(violaciones.isEmpty());
        assertTrue(violaciones.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("minuto")));
    }

    @Test
    void minutoMayorA59_generaViolacion() {
        ConfigurarSchedulerRequest request = new ConfigurarSchedulerRequest(12, 60);

        Set<ConstraintViolation<ConfigurarSchedulerRequest>> violaciones =
                validator.validate(request);

        assertFalse(violaciones.isEmpty());
        assertTrue(violaciones.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("minuto")));
    }
}
