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

class AdminUnidadMedidaRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private AdminUnidadMedidaRequest requestValido() {
        return new AdminUnidadMedidaRequest("Centímetro", "cm", "NUMERICO");
    }

    private Set<ConstraintViolation<AdminUnidadMedidaRequest>> validar(AdminUnidadMedidaRequest request) {
        return validator.validate(request);
    }

    @Test
    void requestValido_sinViolaciones() {
        assertTrue(validar(requestValido()).isEmpty());
    }

    @Test
    void nombreNulo_generaViolacion() {
        Set<ConstraintViolation<AdminUnidadMedidaRequest>> violaciones =
                validar(new AdminUnidadMedidaRequest(null, "cm", "NUMERICO"));

        assertFalse(violaciones.isEmpty());
        assertTrue(violaciones.stream().anyMatch(v -> v.getPropertyPath().toString().equals("nombre")));
    }

    @Test
    void nombreBlanco_generaViolacion() {
        Set<ConstraintViolation<AdminUnidadMedidaRequest>> violaciones =
                validar(new AdminUnidadMedidaRequest("   ", "cm", "NUMERICO"));

        assertFalse(violaciones.isEmpty());
        assertTrue(violaciones.stream().anyMatch(v -> v.getPropertyPath().toString().equals("nombre")));
    }

    @Test
    void nombreMayorA50_generaViolacion() {
        Set<ConstraintViolation<AdminUnidadMedidaRequest>> violaciones =
                validar(new AdminUnidadMedidaRequest("a".repeat(51), "cm", "NUMERICO"));

        assertFalse(violaciones.isEmpty());
        assertTrue(violaciones.stream().anyMatch(v -> v.getPropertyPath().toString().equals("nombre")));
    }

    @Test
    void simboloNulo_generaViolacion() {
        Set<ConstraintViolation<AdminUnidadMedidaRequest>> violaciones =
                validar(new AdminUnidadMedidaRequest("Centímetro", null, "NUMERICO"));

        assertFalse(violaciones.isEmpty());
        assertTrue(violaciones.stream().anyMatch(v -> v.getPropertyPath().toString().equals("simbolo")));
    }

    @Test
    void simboloBlanco_generaViolacion() {
        Set<ConstraintViolation<AdminUnidadMedidaRequest>> violaciones =
                validar(new AdminUnidadMedidaRequest("Centímetro", " ", "NUMERICO"));

        assertFalse(violaciones.isEmpty());
        assertTrue(violaciones.stream().anyMatch(v -> v.getPropertyPath().toString().equals("simbolo")));
    }

    @Test
    void simboloMayorA50_generaViolacion() {
        Set<ConstraintViolation<AdminUnidadMedidaRequest>> violaciones =
                validar(new AdminUnidadMedidaRequest("Centímetro", "s".repeat(51), "NUMERICO"));

        assertFalse(violaciones.isEmpty());
        assertTrue(violaciones.stream().anyMatch(v -> v.getPropertyPath().toString().equals("simbolo")));
    }

    @Test
    void tipoDatoPermitidoNulo_generaViolacion() {
        Set<ConstraintViolation<AdminUnidadMedidaRequest>> violaciones =
                validar(new AdminUnidadMedidaRequest("Centímetro", "cm", null));

        assertFalse(violaciones.isEmpty());
        assertTrue(violaciones.stream().anyMatch(v -> v.getPropertyPath().toString().equals("tipoDatoPermitido")));
    }

    @Test
    void tipoDatoPermitidoBlanco_generaViolacion() {
        Set<ConstraintViolation<AdminUnidadMedidaRequest>> violaciones =
                validar(new AdminUnidadMedidaRequest("Centímetro", "cm", "  "));

        assertFalse(violaciones.isEmpty());
        assertTrue(violaciones.stream().anyMatch(v -> v.getPropertyPath().toString().equals("tipoDatoPermitido")));
    }

    @Test
    void tipoDatoPermitidoMayorA50_generaViolacion() {
        Set<ConstraintViolation<AdminUnidadMedidaRequest>> violaciones =
                validar(new AdminUnidadMedidaRequest("Centímetro", "cm", "t".repeat(51)));

        assertFalse(violaciones.isEmpty());
        assertTrue(violaciones.stream().anyMatch(v -> v.getPropertyPath().toString().equals("tipoDatoPermitido")));
    }
}
