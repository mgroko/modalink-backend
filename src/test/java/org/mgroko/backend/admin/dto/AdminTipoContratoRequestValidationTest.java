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

class AdminTipoContratoRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void createValido_sinViolaciones() {
        AdminTipoContratoCreateRequest request = new AdminTipoContratoCreateRequest(
                "Acuerdo general", "Descripción", "Contrato {{proyecto}}", true);

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void createNombreVacio_generaViolacion() {
        AdminTipoContratoCreateRequest request = new AdminTipoContratoCreateRequest(
                "   ", null, "Contrato", true);

        Set<ConstraintViolation<AdminTipoContratoCreateRequest>> violaciones =
                validator.validate(request);

        assertFalse(violaciones.isEmpty());
        assertTrue(violaciones.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("nombre")));
    }

    @Test
    void createPlantillaVacia_generaViolacion() {
        AdminTipoContratoCreateRequest request = new AdminTipoContratoCreateRequest(
                "Acuerdo", null, "   ", true);

        assertTrue(validator.validate(request).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("plantilla")));
    }

    @Test
    void updateNombreMayorA100_generaViolacion() {
        AdminTipoContratoUpdateRequest request = new AdminTipoContratoUpdateRequest(
                "a".repeat(101), null, "Contrato");

        assertTrue(validator.validate(request).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("nombre")));
    }

    @Test
    void updateDescripcionMayorA200_generaViolacion() {
        AdminTipoContratoUpdateRequest request = new AdminTipoContratoUpdateRequest(
                "Acuerdo", "d".repeat(201), "Contrato");

        assertTrue(validator.validate(request).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("descripcion")));
    }

    @Test
    void estadoNulo_generaViolacion() {
        AdminTipoContratoEstadoRequest request = new AdminTipoContratoEstadoRequest(null);

        assertTrue(validator.validate(request).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("activo")));
    }
}
