package org.mgroko.backend.common.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.actividades.exception.RequerimientoConVinculosException;
import org.mgroko.backend.actividades.exception.TransicionEstadoActividadInvalidaException;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.storage.exception.DimensionesImagenExcedidasException;
import org.mgroko.backend.storage.exception.FormatoImagenInvalidoException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler manejador = new GlobalExceptionHandler();

    @Test
    void dimensionesExcedidas_responde400ConElMensaje() {
        ResponseEntity<Map<String, Object>> respuesta = manejador.handleDimensionesImagenExcedidas(
                new DimensionesImagenExcedidasException("La imagen mide 9000x100 px y supera el máximo de 8000 px por lado."));

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertEquals(400, respuesta.getBody().get("httpStatus"));
        assertTrue(respuesta.getBody().get("message").toString().contains("9000x100"));
    }

    @Test
    void formatoInvalido_responde400ConElMensaje() {
        ResponseEntity<Map<String, Object>> respuesta = manejador.handleFormatoImagenInvalido(
                new FormatoImagenInvalidoException("El archivo no corresponde a una imagen JPEG, PNG o WEBP válida."));

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertEquals(400, respuesta.getBody().get("httpStatus"));
        assertTrue(respuesta.getBody().get("message").toString().contains("JPEG, PNG o WEBP"));
    }

    @Test
    void cicloDetectadoPorTrigger_responde409() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "Error de integridad",
                new RuntimeException(
                        "Dependencia invalida: la actividad 1 como predecesora de 2 genera un ciclo"));

        ResponseEntity<Map<String, Object>> respuesta =
                manejador.handleDataIntegrityViolation(ex);

        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals(409, respuesta.getBody().get("httpStatus"));
        assertTrue(respuesta.getBody().get("message").toString().contains("ciclo"));
    }

    @Test
    void requerimientoConVinculos_responde409() {
        ResponseEntity<Map<String, Object>> respuesta =
                manejador.handleRequerimientoConVinculos(
                        new RequerimientoConVinculosException(7L));

        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals(409, respuesta.getBody().get("httpStatus"));
        assertTrue(respuesta.getBody().get("message").toString()
                .contains("requerimiento 7"));
    }

    @Test
    void transicionDeEstadoInvalida_responde409() {
        ResponseEntity<Map<String, Object>> respuesta =
                manejador.handleTransicionEstadoActividadInvalida(
                        new TransicionEstadoActividadInvalidaException(
                                EstadoActividad.PENDIENTE,
                                EstadoActividad.FINALIZADA));

        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals(409, respuesta.getBody().get("httpStatus"));
        assertTrue(respuesta.getBody().get("message").toString()
                .contains("PENDIENTE a FINALIZADA"));
    }

    @Test
    void violacionDeIntegridadSinCiclo_responde400() {
        ResponseEntity<Map<String, Object>> respuesta =
                manejador.handleDataIntegrityViolation(
                        new DataIntegrityViolationException(
                                "Error de integridad",
                                new RuntimeException("duplicate key value")));

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertEquals(400, respuesta.getBody().get("httpStatus"));
    }
}
