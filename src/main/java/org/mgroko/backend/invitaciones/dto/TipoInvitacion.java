package org.mgroko.backend.invitaciones.dto;

public enum TipoInvitacion {
    /**
     * Invitación al proyecto sin un requerimiento general asociado.
     */
    GENERAL,
    /**
     * Invitación asociada a un requerimiento general del proyecto.
     */
    REQUERIMIENTO_GENERAL,
    ACTIVIDAD
}
