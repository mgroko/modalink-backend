package org.mgroko.backend.proyectos.exception;

import java.util.List;

/**
 * El director no calificó a todos los miembros activos del proyecto.
 */
public class CalificacionIncompletaException extends RuntimeException {

    private final List<Long> idsPerfilFaltantes;

    public CalificacionIncompletaException(List<Long> idsPerfilFaltantes, String mensaje) {
        super(mensaje);
        this.idsPerfilFaltantes = idsPerfilFaltantes == null
                ? List.of()
                : List.copyOf(idsPerfilFaltantes);
    }

    public List<Long> getIdsPerfilFaltantes() {
        return idsPerfilFaltantes;
    }
}
