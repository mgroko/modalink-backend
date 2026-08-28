package org.mgroko.backend.common.exception;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.mgroko.backend.admin.exception.AutoDeshabilitacionException;
import org.mgroko.backend.admin.exception.PerfilNoEncontradoException;
import org.mgroko.backend.admin.exception.UsuarioAdminNoEncontradoException;
import org.mgroko.backend.admin.exception.UsuarioEnBajaException;
import org.mgroko.backend.auth.exception.CorreoDuplicadoException;
import org.mgroko.backend.auth.exception.CredencialesInvalidasException;
import org.mgroko.backend.auth.exception.DniDuplicadoException;
import org.mgroko.backend.auth.exception.EdadInvalidaException;
import org.mgroko.backend.auth.exception.GeneroNoEncontradoException;
import org.mgroko.backend.auth.exception.RolGlobalNoEncontradoException;
import org.mgroko.backend.auth.exception.UsuarioDeshabilitadoException;
import org.mgroko.backend.auth.exception.UsuarioNoEncontradoException;
import org.mgroko.backend.usuario.exception.SolicitudBajaException;
import org.mgroko.backend.usuario.exception.UbicacionNoEncontradaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maneja excepciones de correo duplicado.
     */
    @ExceptionHandler(CorreoDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> handleCorreoDuplicado(CorreoDuplicadoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    /**
     * Maneja excepciones de DNI duplicado.
     */
    @ExceptionHandler(DniDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> handleDniDuplicado(DniDuplicadoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    /**
     * Maneja excepciones de credenciales inválidas.
     */
    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<Map<String, Object>> handleCredencialesInvalidas(CredencialesInvalidasException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    /**
     * Maneja excepciones cuando no se encuentra el rol global.
     */
    @ExceptionHandler(RolGlobalNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleRolGlobalNoEncontrado(RolGlobalNoEncontradoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Maneja excepciones cuando el código de género enviado no existe.
     */
    @ExceptionHandler(GeneroNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleGeneroNoEncontrado(GeneroNoEncontradoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Maneja validaciones fallidas de DTOs (anotaciones @Valid).
     */
     @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> erroresPorCampo = new LinkedHashMap<>();
        for (var fieldError : ex.getBindingResult().getFieldErrors()) {
            erroresPorCampo.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Validación fallida");
        response.put("errores", erroresPorCampo);
        response.put("httpStatus", HttpStatus.BAD_REQUEST.value());
        response.put("timestamp", System.currentTimeMillis());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Helper para construir una respuesta de error estándar.
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        response.put("httpStatus", status.value());
        response.put("timestamp", System.currentTimeMillis());
        return new ResponseEntity<>(response, status);
    }

    /**
     * Excepción de edad inválida.
     */
    @ExceptionHandler(EdadInvalidaException.class)
        public ResponseEntity<Map<String, Object>> handleEdadInvalida(EdadInvalidaException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Excepción de usuario no encontrado.
     */
    @ExceptionHandler(UsuarioNoEncontradoException.class)
        public ResponseEntity<Map<String, Object>> handleUsuarioNoEncontrado(UsuarioNoEncontradoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    /**
     * Excepción de usuario deshabilitado.
     */
    @ExceptionHandler(UsuarioDeshabilitadoException.class)
    public ResponseEntity<Map<String, Object>> handleUsuarioDeshabilitado(UsuarioDeshabilitadoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    /**
     * Excepción de auto deshabilitación.
     */
    @ExceptionHandler(AutoDeshabilitacionException.class)
    public ResponseEntity<Map<String, Object>> handleAutoDeshabilitacion(AutoDeshabilitacionException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    /**
     * Excepción de usuario en baja.
     */
    @ExceptionHandler(UsuarioEnBajaException.class)
    public ResponseEntity<Map<String, Object>> handleUsuarioEnBaja(UsuarioEnBajaException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(UsuarioAdminNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleUsuarioAdminNoEncontrado(UsuarioAdminNoEncontradoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    /**
     * Excepción de perfil no encontrado.
     */
    @ExceptionHandler(PerfilNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handlePerfilNoEncontrado(PerfilNoEncontradoException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UbicacionNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> handleUbicacionNoEncontrada(UbicacionNoEncontradaException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SolicitudBajaException.class)
    public ResponseEntity<Map<String, Object>> handleSolicitudBaja(SolicitudBajaException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

}
