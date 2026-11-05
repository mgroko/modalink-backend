package org.mgroko.backend.proyectos.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class FinalizarProyectoRequestValidationTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void requestValido_noTieneViolaciones() {
        FinalizarProyectoRequest request = new FinalizarProyectoRequest(
                List.of(new CalificacionRequest(20L, 5, "Excelente desempeño")));

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void listaNula_esInvalida() {
        assertThat(validator.validate(new FinalizarProyectoRequest(null)))
                .extracting(violacion -> violacion.getPropertyPath().toString())
                .contains("calificaciones");
    }

    @Test
    void idPerfilNulo_esInvalido() {
        assertThat(validator.validate(new FinalizarProyectoRequest(
                List.of(new CalificacionRequest(null, 5, "Excelente")))))
                .extracting(violacion -> violacion.getPropertyPath().toString())
                .contains("calificaciones[0].idPerfil");
    }

    @Test
    void valoracionNula_esInvalida() {
        assertThat(validator.validate(new FinalizarProyectoRequest(
                List.of(new CalificacionRequest(20L, null, "Excelente")))))
                .extracting(violacion -> violacion.getPropertyPath().toString())
                .contains("calificaciones[0].valoracion");
    }

    @Test
    void valoracionCero_esInvalida() {
        assertThat(validator.validate(new FinalizarProyectoRequest(
                List.of(new CalificacionRequest(20L, 0, "Excelente")))))
                .extracting(violacion -> violacion.getPropertyPath().toString())
                .contains("calificaciones[0].valoracion");
    }

    @Test
    void valoracionSeis_esInvalida() {
        assertThat(validator.validate(new FinalizarProyectoRequest(
                List.of(new CalificacionRequest(20L, 6, "Excelente")))))
                .extracting(violacion -> violacion.getPropertyPath().toString())
                .contains("calificaciones[0].valoracion");
    }

    @Test
    void reseniaNula_esInvalida() {
        assertThat(validator.validate(new FinalizarProyectoRequest(
                List.of(new CalificacionRequest(20L, 5, null)))))
                .extracting(violacion -> violacion.getPropertyPath().toString())
                .contains("calificaciones[0].resenia");
    }

    @Test
    void reseniaVacia_esInvalida() {
        assertThat(validator.validate(new FinalizarProyectoRequest(
                List.of(new CalificacionRequest(20L, 5, "   ")))))
                .extracting(violacion -> violacion.getPropertyPath().toString())
                .contains("calificaciones[0].resenia");
    }

    @Test
    void reseniaDe200Caracteres_esValida() {
        assertThat(validator.validate(new FinalizarProyectoRequest(
                List.of(new CalificacionRequest(20L, 5, "x".repeat(200))))))
                .isEmpty();
    }

    @Test
    void reseniaDe201Caracteres_esInvalida() {
        assertThat(validator.validate(new FinalizarProyectoRequest(
                List.of(new CalificacionRequest(20L, 5, "x".repeat(201))))))
                .extracting(violacion -> violacion.getPropertyPath().toString())
                .contains("calificaciones[0].resenia");
    }
}
