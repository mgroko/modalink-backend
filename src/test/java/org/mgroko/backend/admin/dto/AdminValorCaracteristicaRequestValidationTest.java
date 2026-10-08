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

class AdminValorCaracteristicaRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void requestValido_conColorHex_sinViolaciones() {
        AdminValorCaracteristicaRequest request = new AdminValorCaracteristicaRequest(
                1L, "AZUL", "#0000FF");

        Set<ConstraintViolation<AdminValorCaracteristicaRequest>> violaciones =
                validator.validate(request);

        assertTrue(violaciones.isEmpty());
    }

    @Test
    void requestValido_sinColorHex_sinViolaciones() {
        AdminValorCaracteristicaRequest request = new AdminValorCaracteristicaRequest(
                null, "RUBIO", null);

        Set<ConstraintViolation<AdminValorCaracteristicaRequest>> violaciones =
                validator.validate(request);

        assertTrue(violaciones.isEmpty());
    }

    @Test
    void codigoBlanco_generaViolacion() {
        AdminValorCaracteristicaRequest request = new AdminValorCaracteristicaRequest(
                null, "   ", "#000000");

        Set<ConstraintViolation<AdminValorCaracteristicaRequest>> violaciones =
                validator.validate(request);

        assertFalse(violaciones.isEmpty());
        assertTrue(violaciones.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("codigo")));
    }

    @Test
    void colorHexFormatoInvalido_generaViolacion() {
        AdminValorCaracteristicaRequest request = new AdminValorCaracteristicaRequest(
                null, "ROJO", "rojo-invalido");

        Set<ConstraintViolation<AdminValorCaracteristicaRequest>> violaciones =
                validator.validate(request);

        assertFalse(violaciones.isEmpty());
        assertTrue(violaciones.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("colorHex")));
    }

    @Test
    void colorHexSinNumeral_generaViolacion() {
        AdminValorCaracteristicaRequest request = new AdminValorCaracteristicaRequest(
                null, "VERDE", "00FF00");

        Set<ConstraintViolation<AdminValorCaracteristicaRequest>> violaciones =
                validator.validate(request);

        assertFalse(violaciones.isEmpty());
        assertTrue(violaciones.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("colorHex")));
    }
}
