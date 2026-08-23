package org.mgroko.backend.auth.dto;

import java.time.LocalDate;
import java.time.Month;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Prueba las anotaciones de Bean Validation de RegistroRequest y
 * LoginRequest sin levantar el contexto de Spring: se valida el record
 * directamente con el Validator de Jakarta. Cada @Test cubre un valor
 * límite distinto por campo (equivalente a una fila de tabla de decisión).
 */
class AuthDTOValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private RegistroRequest registroValido() {
        return new RegistroRequest(
                "Juan", "Perez", "12345678",
                LocalDate.of(1995, Month.MAY, 20),
                "juan.perez@test.com", "mujer", "password123"
        );
    }

    @Test
    void registro_datosValidos_sinViolaciones() {
        Set<ConstraintViolation<RegistroRequest>> violaciones = validator.validate(registroValido());
        assertTrue(violaciones.isEmpty());
    }

    @Test
    void registro_nombreVacio_generaViolacion() {
        RegistroRequest request = new RegistroRequest(
                "", "Perez", "12345678",
                LocalDate.of(1995, Month.MAY, 20), "juan.perez@test.com", "mujer", "password123"
        );
        Set<ConstraintViolation<RegistroRequest>> violaciones = validator.validate(request);
        assertTrue(violaciones.stream().anyMatch(v -> v.getPropertyPath().toString().equals("nombre")));
    }

    @Test
    void registro_nombreDemasiadoCorto_generaViolacion() {
        // @Size(min = 2) -> "J" tiene 1 caracter
        RegistroRequest request = new RegistroRequest(
                "J", "Perez", "12345678",
                LocalDate.of(1995, Month.MAY, 20), "juan.perez@test.com", "mujer", "password123"
        );
        Set<ConstraintViolation<RegistroRequest>> violaciones = validator.validate(request);
        assertEquals(1, violaciones.size());
    }

    @Test
    void registro_nombreDemasiadoLargo_generaViolacion() {
        // @Size(max = 50) -> 51 caracteres
        String nombreLargo = "J".repeat(51);
        RegistroRequest request = new RegistroRequest(
                nombreLargo, "Perez", "12345678",
                LocalDate.of(1995, Month.MAY, 20), "juan.perez@test.com", "mujer", "password123"
        );
        Set<ConstraintViolation<RegistroRequest>> violaciones = validator.validate(request);
        assertEquals(1, violaciones.size());
    }

    @Test
    void registro_correoSinArroba_generaViolacion() {
        RegistroRequest request = new RegistroRequest(
                "Juan", "Perez", "12345678",
                LocalDate.of(1995, Month.MAY, 20), "correo-invalido", "mujer", "password123"
        );
        Set<ConstraintViolation<RegistroRequest>> violaciones = validator.validate(request);
        assertTrue(violaciones.stream().anyMatch(v -> v.getPropertyPath().toString().equals("correo")));
    }

    @Test
    void registro_correoVacio_generaUnaViolacion() {
        RegistroRequest request = new RegistroRequest(
                "Juan", "Perez", "12345678",
                LocalDate.of(1995, Month.MAY, 20), "", "mujer", "password123"
        );
        Set<ConstraintViolation<RegistroRequest>> violaciones = validator.validate(request);
        assertEquals(1, violaciones.size());
        assertTrue(violaciones.stream().anyMatch(v -> v.getPropertyPath().toString().equals("correo")));
    }

    @Test
    void registro_generoVacio_generaViolacion() {
        RegistroRequest request = new RegistroRequest(
                "Juan", "Perez", "12345678",
                LocalDate.of(1995, Month.MAY, 20), "juan.perez@test.com", "", "password123"
        );
        Set<ConstraintViolation<RegistroRequest>> violaciones = validator.validate(request);
        assertTrue(violaciones.stream().anyMatch(v -> v.getPropertyPath().toString().equals("genero")));
    }

    @Test
    void registro_fechaNacimientoFutura_generaViolacion() {
        // @Past no admite fechas futuras
        RegistroRequest request = new RegistroRequest(
                "Juan", "Perez", "12345678",
                LocalDate.now().plusDays(1), "juan.perez@test.com", "mujer", "password123"
        );
        Set<ConstraintViolation<RegistroRequest>> violaciones = validator.validate(request);
        assertTrue(violaciones.stream().anyMatch(v -> v.getPropertyPath().toString().equals("fechaNacimiento")));
    }

    @Test
    void registro_dniVacio_generaViolacion() {
        RegistroRequest request = new RegistroRequest(
                "Juan", "Perez", "",
                LocalDate.of(1995, Month.MAY, 20), "juan.perez@test.com", "mujer", "password123"
        );
        Set<ConstraintViolation<RegistroRequest>> violaciones = validator.validate(request);
        assertTrue(violaciones.stream().anyMatch(v -> v.getPropertyPath().toString().equals("dni")));
    }

    @Test
    void registro_passwordVacia_generaViolacion() {
        RegistroRequest request = new RegistroRequest(
                "Juan", "Perez", "12345678",
                LocalDate.of(1995, Month.MAY, 20), "juan.perez@test.com", "mujer", ""
        );
        Set<ConstraintViolation<RegistroRequest>> violaciones = validator.validate(request);
        assertTrue(violaciones.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    // -----------------------------------------------------------
    // LoginRequest
    // -----------------------------------------------------------

    @Test
    void login_datosValidos_sinViolaciones() {
        LoginRequest request = new LoginRequest("juan.perez@test.com", "password123");
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void login_correoInvalido_generaViolacion() {
        LoginRequest request = new LoginRequest("no-es-un-correo", "password123");
        Set<ConstraintViolation<LoginRequest>> violaciones = validator.validate(request);
        assertTrue(violaciones.stream().anyMatch(v -> v.getPropertyPath().toString().equals("correo")));
    }

    @Test
    void login_passwordVacia_generaViolacion() {
        LoginRequest request = new LoginRequest("juan.perez@test.com", "");
        Set<ConstraintViolation<LoginRequest>> violaciones = validator.validate(request);
        assertTrue(violaciones.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }
}