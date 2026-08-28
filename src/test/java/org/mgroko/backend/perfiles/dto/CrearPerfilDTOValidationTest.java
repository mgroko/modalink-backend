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

class CrearPerfilDTOValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private CrearPerfilRequest requestValido() {
        return new CrearPerfilRequest(
                "Luna", 2L, "Modelo profesional.",
                List.of(new CaracteristicaPerfilRequest(11L, "175")));
    }

    @Test
    void crear_datosValidos_sinViolaciones() {
        assertTrue(validator.validate(requestValido()).isEmpty());
    }

    @Test
    void crear_nombreArtisticoVacio_generaViolacion() {
        CrearPerfilRequest request = new CrearPerfilRequest(
                "", 2L, "Modelo profesional.", List.of());
        Set<ConstraintViolation<CrearPerfilRequest>> violaciones = validator.validate(request);
        assertTrue(violaciones.stream().anyMatch(v -> v.getPropertyPath().toString().equals("nombreArtistico")));
    }

    @Test
    void crear_nombreArtisticoDemasiadoCorto_generaViolacion() {
        CrearPerfilRequest request = new CrearPerfilRequest(
                "L", 2L, "Modelo profesional.", List.of());
        Set<ConstraintViolation<CrearPerfilRequest>> violaciones = validator.validate(request);
        assertEquals(1, violaciones.size());
    }

    @Test
    void crear_nombreArtisticoDemasiadoLargo_generaViolacion() {
        CrearPerfilRequest request = new CrearPerfilRequest(
                "L".repeat(51), 2L, "Modelo profesional.", List.of());
        Set<ConstraintViolation<CrearPerfilRequest>> violaciones = validator.validate(request);
        assertEquals(1, violaciones.size());
    }

    @Test
    void crear_profesionNula_generaViolacion() {
        CrearPerfilRequest request = new CrearPerfilRequest(
                "Luna", null, "Modelo profesional.", List.of());
        Set<ConstraintViolation<CrearPerfilRequest>> violaciones = validator.validate(request);
        assertTrue(violaciones.stream().anyMatch(v -> v.getPropertyPath().toString().equals("idProfesion")));
    }

    @Test
    void crear_biografiaVacia_generaViolacion() {
        CrearPerfilRequest request = new CrearPerfilRequest(
                "Luna", 2L, "", List.of());
        Set<ConstraintViolation<CrearPerfilRequest>> violaciones = validator.validate(request);
        assertTrue(violaciones.stream().anyMatch(v -> v.getPropertyPath().toString().equals("biografia")));
    }

    @Test
    void crear_biografiaDemasiadoLarga_generaViolacion() {
        CrearPerfilRequest request = new CrearPerfilRequest(
                "Luna", 2L, "B".repeat(501), List.of());
        Set<ConstraintViolation<CrearPerfilRequest>> violaciones = validator.validate(request);
        assertEquals(1, violaciones.size());
    }

    @Test
    void crear_caracteristicaSinId_generaViolacion() {
        CrearPerfilRequest request = new CrearPerfilRequest(
                "Luna", 2L, "Modelo profesional.",
                List.of(new CaracteristicaPerfilRequest(null, "175")));
        Set<ConstraintViolation<CrearPerfilRequest>> violaciones = validator.validate(request);
        assertTrue(violaciones.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("caracteristicas[0].idCaracteristica")));
    }

    @Test
    void crear_caracteristicaValorNulo_sinViolaciones() {
        CrearPerfilRequest request = new CrearPerfilRequest(
                "Luna", 2L, "Modelo profesional.",
                List.of(new CaracteristicaPerfilRequest(11L, null)));
        assertTrue(validator.validate(request).isEmpty());
    }
}