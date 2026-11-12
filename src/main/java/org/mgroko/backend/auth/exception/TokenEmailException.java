package org.mgroko.backend.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * Error controlado del flujo de auto-login por email.
 */
public class TokenEmailException extends RuntimeException {

    private final String codigo;
    private final HttpStatus httpStatus;

    public TokenEmailException(String codigo, String mensaje, HttpStatus httpStatus) {
        super(mensaje);
        this.codigo = codigo;
        this.httpStatus = httpStatus;
    }

    public String getCodigo() {
        return codigo;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public static TokenEmailException requerido() {
        return new TokenEmailException(
                "TOKEN_REQUERIDO",
                "El enlace no incluye un token de acceso.",
                HttpStatus.BAD_REQUEST);
    }

    public static TokenEmailException invalido() {
        return new TokenEmailException(
                "TOKEN_INVALIDO",
                "El enlace de acceso no es válido.",
                HttpStatus.UNAUTHORIZED);
    }

    public static TokenEmailException expirado() {
        return new TokenEmailException(
                "TOKEN_EXPIRADO",
                "El enlace de acceso venció. Iniciá sesión nuevamente.",
                HttpStatus.GONE);
    }

    public static TokenEmailException usado() {
        return new TokenEmailException(
                "TOKEN_USADO",
                "El enlace de acceso ya fue utilizado.",
                HttpStatus.CONFLICT);
    }

    public static TokenEmailException usuarioNoActivo() {
        return new TokenEmailException(
                "USUARIO_NO_ACTIVO",
                "La cuenta asociada al enlace no está habilitada.",
                HttpStatus.FORBIDDEN);
    }
}
