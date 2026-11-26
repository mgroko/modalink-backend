package org.mgroko.backend.proyectos.dto;

/**
 * Estado visual de un área del informe de proyecto.
 *
 * <p>No existe un puntaje agregado: el estado surge de las alertas del área.
 * Un área sin datos nunca se representa como {@link #VERDE}.</p>
 */
public enum EstadoAreaInforme {

    ROJO,
    AMARILLO,
    VERDE,
    SIN_DATOS,
    NO_APLICA;

    public String getNombre() {
        return switch (this) {
            case ROJO -> "Rojo";
            case AMARILLO -> "Amarillo";
            case VERDE -> "Verde";
            case SIN_DATOS -> "Sin datos";
            case NO_APLICA -> "No aplica";
        };
    }
}
