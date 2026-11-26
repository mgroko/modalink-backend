package org.mgroko.backend.proyectos.dto;

/**
 * Severidad de una alerta del informe de proyecto.
 */
public enum SeveridadInforme {

    ALTA,
    MEDIA,
    BAJA,
    INFO;

    public String getNombre() {
        return switch (this) {
            case ALTA -> "Alta";
            case MEDIA -> "Media";
            case BAJA -> "Baja";
            case INFO -> "Informativa";
        };
    }
}
