package org.mgroko.backend.admin.dto;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class AdminCaracteristicaTecnicaRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private AdminCaracteristicaTecnicaRequest requestValido() {
        return new AdminCaracteristicaTecnicaRequest(
                "altura",
                "cm",
                1L,
                "NUMERICO",
                Collections.emptyList()
        );
    }

    @Test
    void requestValido_sinViolaciones() {
        Set<ConstraintViolation<AdminCaracteristicaTecnicaRequest>> violaciones =
                validator.validate(requestValido());
        assertTrue(violaciones.isEmpty());
    }

    @Test
    void codigoNulo_generaViolacion() {
        AdminCaracteristicaTecnicaRequest request = new AdminCaracteristicaTecnicaRequest(
                null, "cm", 1L, "NUMERICO", Collections.emptyList());

        Set<ConstraintViolation<AdminCaracteristicaTecnicaRequest>> violaciones =
                validator.validate(request);

        assertFalse(violaciones.isEmpty());
        assertTrue(violaciones.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("codigo")));
    }

    @Test
    void codigoBlanco_generaViolacion() {
        AdminCaracteristicaTecnicaRequest request = new AdminCaracteristicaTecnicaRequest(
                "   ", "cm", 1L, "NUMERICO", Collections.emptyList());

        Set<ConstraintViolation<AdminCaracteristicaTecnicaRequest>> violaciones =
                validator.validate(request);

        assertFalse(violaciones.isEmpty());
        assertTrue(violaciones.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("codigo")));
    }

    @Test
    void codigoMayorA50_generaViolacion() {
        String codigoLargo = "a".repeat(51);
        AdminCaracteristicaTecnicaRequest request = new AdminCaracteristicaTecnicaRequest(
                codigoLargo, "cm", 1L, "NUMERICO", Collections.emptyList());

        Set<ConstraintViolation<AdminCaracteristicaTecnicaRequest>> violaciones =
                validator.validate(request);

        assertFalse(violaciones.isEmpty());
        assertTrue(violaciones.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("codigo")));
    }

    @Test
    void idProfesionNulo_generaViolacion() {
        AdminCaracteristicaTecnicaRequest request = new AdminCaracteristicaTecnicaRequest(
                "altura", "cm", null, "NUMERICO", Collections.emptyList());

        Set<ConstraintViolation<AdminCaracteristicaTecnicaRequest>> violaciones =
                validator.validate(request);

        assertFalse(violaciones.isEmpty());
        assertTrue(violaciones.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("idProfesion")));
    }

    @Test
    void tipoDatoBlanco_generaViolacion() {
        AdminCaracteristicaTecnicaRequest request = new AdminCaracteristicaTecnicaRequest(
                "altura", "cm", 1L, "  ", Collections.emptyList());

        Set<ConstraintViolation<AdminCaracteristicaTecnicaRequest>> violaciones =
                validator.validate(request);

        assertFalse(violaciones.isEmpty());
        assertTrue(violaciones.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("tipoDato")));
    }

    @Test
    void valorCaracteristicaInvalidoEnLista_generaViolacionCascada() {
        AdminValorCaracteristicaRequest valorInvalido = new AdminValorCaracteristicaRequest(
                null, "   ", "#FFFFFF");
        AdminCaracteristicaTecnicaRequest request = new AdminCaracteristicaTecnicaRequest(
                "ojos", null, 1L, "ENUMERADO", List.of(valorInvalido));

        Set<ConstraintViolation<AdminCaracteristicaTecnicaRequest>> violaciones =
                validator.validate(request);

        assertFalse(violaciones.isEmpty());
        assertTrue(violaciones.stream()
                .anyMatch(v -> v.getPropertyPath().toString().startsWith("valores[0]")));
    }
}
