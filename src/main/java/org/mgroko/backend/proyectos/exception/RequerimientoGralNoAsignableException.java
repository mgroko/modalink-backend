package org.mgroko.backend.proyectos.exception;

/**
 * Un requerimiento general no puede vincularse a un miembro por una regla de
 * negocio: profesión incompatible, proyecto incorrecto o estado no permitido.
 */
public class RequerimientoGralNoAsignableException extends RuntimeException {

    private final String codigo;

    public RequerimientoGralNoAsignableException(String mensaje, String codigo) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
