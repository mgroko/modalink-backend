package org.mgroko.backend.usuario.dto;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class DatosPersonalesDTOValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private DatosPersonalesRequest datosValidos() {
        return new DatosPersonalesRequest(
                "Juan", "Perez",
                LocalDate.of(1995, 5, 20),
                "mujer", null);
    }

    @Test
    void datosValidos_sinViolaciones() {
        Set<ConstraintViolation<DatosPersonalesRequest>> violaciones = validator.validate(datosValidos());
        assertTrue(violaciones.isEmpty());
    }

    @Test
    void nombreVacio_generaViolacion() {
        DatosPersonalesRequest request = new DatosPersonalesRequest(
                "", "Perez",
                LocalDate.of(1995, 5, 20),
                "mujer", null);
        Set<ConstraintViolation<DatosPersonalesRequest>> violaciones = validator.validate(request);
        assertTrue(violaciones.stream().anyMatch(v -> v.getPropertyPath().toString().equals("nombre")));
    }

    @Test
    void nombreDemasiadoCorto_generaViolacion() {
        DatosPersonalesRequest request = new DatosPersonalesRequest(
                "J", "Perez",
                LocalDate.of(1995, 5, 20),
                "mujer", null);
        Set<ConstraintViolation<DatosPersonalesRequest>> violaciones = validator.validate(request);
        assertTrue(violaciones.stream().anyMatch(v -> v.getPropertyPath().toString().equals("nombre")));
    }

    @Test
    void nombreDemasiadoLargo_generaViolacion() {
        String nombreLargo = "J".repeat(51);
        DatosPersonalesRequest request = new DatosPersonalesRequest(
                nombreLargo, "Perez",
                LocalDate.of(1995, 5, 20),
                "mujer", null);
        Set<ConstraintViolation<DatosPersonalesRequest>> violaciones = validator.validate(request);
        assertTrue(violaciones.stream().anyMatch(v -> v.getPropertyPath().toString().equals("nombre")));
    }

    @Test
    void apellidoVacio_generaViolacion() {
        DatosPersonalesRequest request = new DatosPersonalesRequest(
                "Juan", "",
                LocalDate.of(1995, 5, 20),
                "mujer", null);
        Set<ConstraintViolation<DatosPersonalesRequest>> violaciones = validator.validate(request);
        assertTrue(violaciones.stream().anyMatch(v -> v.getPropertyPath().toString().equals("apellido")));
    }

    @Test
    void apellidoDemasiadoCorto_generaViolacion() {
        DatosPersonalesRequest request = new DatosPersonalesRequest(
                "Juan", "P",
                LocalDate.of(1995, 5, 20),
                "mujer", null);
        Set<ConstraintViolation<DatosPersonalesRequest>> violaciones = validator.validate(request);
        assertTrue(violaciones.stream().anyMatch(v -> v.getPropertyPath().toString().equals("apellido")));
    }

    @Test
    void fechaNacimientoNula_generaViolacion() {
        DatosPersonalesRequest request = new DatosPersonalesRequest(
                "Juan", "Perez",
                null,
                "mujer", null);
        Set<ConstraintViolation<DatosPersonalesRequest>> violaciones = validator.validate(request);
        assertTrue(violaciones.stream().anyMatch(v -> v.getPropertyPath().toString().equals("fechaNacimiento")));
    }

    @Test
    void fechaNacimientoFutura_generaViolacion() {
        DatosPersonalesRequest request = new DatosPersonalesRequest(
                "Juan", "Perez",
                LocalDate.now().plusDays(1),
                "mujer", null);
        Set<ConstraintViolation<DatosPersonalesRequest>> violaciones = validator.validate(request);
        assertTrue(violaciones.stream().anyMatch(v -> v.getPropertyPath().toString().equals("fechaNacimiento")));
    }
}
