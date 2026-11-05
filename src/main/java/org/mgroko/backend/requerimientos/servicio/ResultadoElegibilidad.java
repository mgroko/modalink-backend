package org.mgroko.backend.requerimientos.servicio;

import org.mgroko.backend.requerimientos.dto.MotivoNoPostulable;

/**
 * Resultado de evaluar si el perfil activo puede postularse estructuralmente a
 * un requerimiento.
 */
public record ResultadoElegibilidad(boolean puedePostularse, MotivoNoPostulable motivo) {

    public static ResultadoElegibilidad habilitado() {
        return new ResultadoElegibilidad(true, null);
    }

    public static ResultadoElegibilidad bloqueado(MotivoNoPostulable motivo) {
        return new ResultadoElegibilidad(false, motivo);
    }
}
