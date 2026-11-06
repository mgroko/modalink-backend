package org.mgroko.backend.reemplazos.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Manejo de errores acotado a los controladores del módulo de reemplazos.
 */
@RestControllerAdvice(basePackages = "org.mgroko.backend.reemplazos.controlador")
public class ReemplazoExceptionHandler {

    @ExceptionHandler(ReemplazoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> noEncontrado(
            ReemplazoNoEncontradoException ex) {
        return responder(ex.getMessage(), "REEMPLAZO_NO_ENCONTRADO", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(CandidatoReemplazoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> candidatoNoEncontrado(
            CandidatoReemplazoNoEncontradoException ex) {
        return responder(ex.getMessage(), "CANDIDATO_REEMPLAZO_NO_ENCONTRADO", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ReemplazoNoPermitidoException.class)
    public ResponseEntity<Map<String, Object>> noPermitido(
            ReemplazoNoPermitidoException ex) {
        return responder(ex.getMessage(), ex.getCodigo(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ReemplazoNoGestionableException.class)
    public ResponseEntity<Map<String, Object>> noGestionable(
            ReemplazoNoGestionableException ex) {
        return responder(ex.getMessage(), ex.getCodigo(), HttpStatus.CONFLICT);
    }

    private ResponseEntity<Map<String, Object>> responder(
            String mensaje,
            String codigo,
            HttpStatus status) {

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", mensaje);
        response.put("codigo", codigo);
        response.put("httpStatus", status.value());
        response.put("timestamp", System.currentTimeMillis());
        return new ResponseEntity<>(response, status);
    }
}
