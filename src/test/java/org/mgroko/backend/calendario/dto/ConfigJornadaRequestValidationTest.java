package org.mgroko.backend.calendario.dto;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class ConfigJornadaRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void valido_jornadaCorrida_sinViolaciones() {
        ConfigJornadaRequest request = new ConfigJornadaRequest(60, List.of(
                new JornadaDiaRequest(1, LocalTime.of(9, 0), null, null, LocalTime.of(18, 0))));

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void valido_jornadaPartida_sinViolaciones() {
        ConfigJornadaRequest request = new ConfigJornadaRequest(60, List.of(
                new JornadaDiaRequest(1, LocalTime.of(9, 0), LocalTime.of(13, 0),
                        LocalTime.of(15, 0), LocalTime.of(19, 0))));

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void diasVacio_generaViolacion() {
        ConfigJornadaRequest request = new ConfigJornadaRequest(60, List.of());

        assertTrue(validator.validate(request).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("dias")));
    }

    @Test
    void margenNulo_generaViolacion() {
        ConfigJornadaRequest request = new ConfigJornadaRequest(null, List.of(
                new JornadaDiaRequest(1, LocalTime.of(9, 0), null, null, LocalTime.of(18, 0))));

        assertTrue(validator.validate(request).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("margenActividadMinutos")));
    }

    @Test
    void diaSinDiaSemana_generaViolacion() {
        ConfigJornadaRequest request = new ConfigJornadaRequest(60, List.of(
                new JornadaDiaRequest(null, LocalTime.of(9, 0), null, null, LocalTime.of(18, 0))));

        assertTrue(validator.validate(request).stream()
                .anyMatch(v -> v.getPropertyPath().toString().contains("diaSemana")));
    }

    @Test
    void diaSinExtremosObligatorios_generaViolacion() {
        ConfigJornadaRequest request = new ConfigJornadaRequest(60, List.of(
                new JornadaDiaRequest(1, null, null, null, null)));

        assertTrue(validator.validate(request).stream()
                .anyMatch(v -> v.getPropertyPath().toString().contains("horarioInicioManiana")));
        assertTrue(validator.validate(request).stream()
                .anyMatch(v -> v.getPropertyPath().toString().contains("horarioFinTarde")));
    }
}
