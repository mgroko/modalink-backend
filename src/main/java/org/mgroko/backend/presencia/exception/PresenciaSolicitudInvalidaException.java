package org.mgroko.backend.presencia.exception;

/**
 * Los IDs de perfil enviados a los endpoints de presencia son inválidos o
 * superan el máximo permitido.
 */
public class PresenciaSolicitudInvalidaException extends RuntimeException {

    private final String codigo;

    public PresenciaSolicitudInvalidaException(String mensaje) {
        this(mensaje, "IDS_PERFIL_INVALIDOS");
    }

    public PresenciaSolicitudInvalidaException(String mensaje, String codigo) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
