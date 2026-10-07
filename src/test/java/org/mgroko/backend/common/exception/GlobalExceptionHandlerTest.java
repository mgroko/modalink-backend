package org.mgroko.backend.common.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.storage.exception.DimensionesImagenExcedidasException;
import org.mgroko.backend.storage.exception.FormatoImagenInvalidoException;
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
}
