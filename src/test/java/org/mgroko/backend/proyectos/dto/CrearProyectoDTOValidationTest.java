package org.mgroko.backend.proyectos.dto;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.enums.Privacidad;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class CrearProyectoDTOValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void requestValido_noTieneViolaciones() {
        CrearProyectoRequest request = new CrearProyectoRequest(
                "Desfile Primavera",
                "Colección primavera-verano",
                Privacidad.Publico,
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(20),
                true,
                null,
                null
        );

        Set<ConstraintViolation<CrearProyectoRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    void requestConCamposObligatoriosNulosOVacios_tieneViolaciones() {
        CrearProyectoRequest request = new CrearProyectoRequest(
                "",
                "   ",
                null,
                null,
                null,
                false,
                null,
                null
        );

        Set<ConstraintViolation<CrearProyectoRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(4);
    }

    @Test
    void requestConNombreMuyLargo_tieneViolacion() {
        String nombreLargo = "A".repeat(51);
        CrearProyectoRequest request = new CrearProyectoRequest(
                nombreLargo,
                "Descripción válida",
                Privacidad.Privado,
                LocalDate.now(),
                null,
                false,
                null,
                null
        );

        Set<ConstraintViolation<CrearProyectoRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("no puede superar los 50 caracteres");
    }

    @Test
    void requestConDescripcionMuyLarga_tieneViolacion() {
        String descLarga = "A".repeat(201);
        CrearProyectoRequest request = new CrearProyectoRequest(
                "Proyecto Valido",
                descLarga,
                Privacidad.Privado,
                LocalDate.now(),
                null,
                false,
                null,
                null
        );

        Set<ConstraintViolation<CrearProyectoRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("no puede superar los 200 caracteres");
    }
}
