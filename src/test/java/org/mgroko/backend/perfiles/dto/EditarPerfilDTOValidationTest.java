package org.mgroko.backend.perfiles.dto;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class EditarPerfilDTOValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private EditarPerfilRequest requestValido() {
        return new EditarPerfilRequest(
                "Luna", "Modelo profesional.",
                null, List.of(new CaracteristicaPerfilRequest(11L, "175", null)));
    }

    @Test
    void editar_datosValidos_sinViolaciones() {
        assertTrue(validator.validate(requestValido()).isEmpty());
    }

    @Test
    void editar_nombreArtisticoVacio_generaViolacion() {
        EditarPerfilRequest request = new EditarPerfilRequest("", "Modelo.", null, List.of());
        Set<ConstraintViolation<EditarPerfilRequest>> violaciones = validator.validate(request);
        assertTrue(violaciones.stream().anyMatch(v -> v.getPropertyPath().toString().equals("nombreArtistico")));
    }

    @Test
    void editar_nombreArtisticoDemasiadoLargo_generaViolacion() {
        EditarPerfilRequest request = new EditarPerfilRequest("L".repeat(51), "Modelo.", null, List.of());
        Set<ConstraintViolation<EditarPerfilRequest>> violaciones = validator.validate(request);
        assertEquals(1, violaciones.size());
    }

    @Test
    void editar_biografiaVacia_generaViolacion() {
        EditarPerfilRequest request = new EditarPerfilRequest("Luna", "", null, List.of());
        Set<ConstraintViolation<EditarPerfilRequest>> violaciones = validator.validate(request);
        assertTrue(violaciones.stream().anyMatch(v -> v.getPropertyPath().toString().equals("biografia")));
    }

    @Test
    void editar_caracteristicaSinId_generaViolacion() {
        EditarPerfilRequest request = new EditarPerfilRequest(
                "Luna", "Modelo.", null, List.of(new CaracteristicaPerfilRequest(null, "175", null)));
        Set<ConstraintViolation<EditarPerfilRequest>> violaciones = validator.validate(request);
        assertTrue(violaciones.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("caracteristicas[0].idCaracteristica")));
    }

    @Test
    void editar_idImagenNulo_sinViolaciones() {
        EditarPerfilRequest request = new EditarPerfilRequest("Luna", "Modelo.", null, List.of());
        assertTrue(validator.validate(request).isEmpty());
    }
}