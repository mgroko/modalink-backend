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

class AdminHabilidadRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private AdminHabilidadRequest requestValido() {
        return new AdminHabilidadRequest("Fotografía de Estudio", "Iluminación y sets.");
    }

    private Set<ConstraintViolation<AdminHabilidadRequest>> validar(AdminHabilidadRequest request) {
        return validator.validate(request);
    }

    @Test
    void requestValido_sinViolaciones() {
        assertTrue(validar(requestValido()).isEmpty());
    }

    @Test
    void nombreNulo_generaViolacion() {
        Set<ConstraintViolation<AdminHabilidadRequest>> violaciones =
                validar(new AdminHabilidadRequest(null, "Descripción"));

        assertFalse(violaciones.isEmpty());
        assertTrue(violaciones.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("nombre")));
    }

    @Test
    void nombreBlanco_generaViolacion() {
        Set<ConstraintViolation<AdminHabilidadRequest>> violaciones =
                validar(new AdminHabilidadRequest("   ", "Descripción"));

        assertFalse(violaciones.isEmpty());
        assertTrue(violaciones.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("nombre")));
    }

    @Test
    void nombreMayorA255_generaViolacion() {
        Set<ConstraintViolation<AdminHabilidadRequest>> violaciones =
                validar(new AdminHabilidadRequest("a".repeat(256), null));

        assertFalse(violaciones.isEmpty());
        assertTrue(violaciones.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("nombre")));
    }

    @Test
    void descripcionMayorA255_generaViolacion() {
        Set<ConstraintViolation<AdminHabilidadRequest>> violaciones =
                validar(new AdminHabilidadRequest("Habilidad", "d".repeat(256)));

        assertFalse(violaciones.isEmpty());
        assertTrue(violaciones.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("descripcion")));
    }
}
