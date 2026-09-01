package org.mgroko.backend.calendario.dto;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class MarcarNoDisponibleRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private MarcarNoDisponibleRequest request(
            LocalDateTime inicio, LocalDateTime fin, String motivo) {
        return new MarcarNoDisponibleRequest(inicio, fin, motivo);
    }

    @Test
    void valido_sinMotivo_sinViolaciones() {
        MarcarNoDisponibleRequest request = request(
                LocalDateTime.of(2026, 9, 15, 10, 0),
                LocalDateTime.of(2026, 9, 15, 14, 0),
                null);

        Set<ConstraintViolation<MarcarNoDisponibleRequest>> violaciones = validator.validate(request);

        assertTrue(violaciones.isEmpty());
    }

    @Test
    void valido_conMotivo_sinViolaciones() {
        MarcarNoDisponibleRequest request = request(
                LocalDateTime.of(2026, 9, 15, 10, 0),
                LocalDateTime.of(2026, 9, 15, 14, 0),
                "Vacaciones");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void inicioNulo_generaViolacion() {
        MarcarNoDisponibleRequest request = request(
                null, LocalDateTime.of(2026, 9, 15, 14, 0), null);

        assertTrue(validator.validate(request).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("fechaHoraInicio")));
    }

    @Test
    void finNulo_generaViolacion() {
        MarcarNoDisponibleRequest request = request(
                LocalDateTime.of(2026, 9, 15, 10, 0), null, null);

        assertTrue(validator.validate(request).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("fechaHoraFin")));
    }

    @Test
    void motivoMayorA200_generaViolacion() {
        MarcarNoDisponibleRequest request = request(
                LocalDateTime.of(2026, 9, 15, 10, 0),
                LocalDateTime.of(2026, 9, 15, 14, 0),
                "a".repeat(201));

        assertTrue(validator.validate(request).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("motivo")));
    }
}