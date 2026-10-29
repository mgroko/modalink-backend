package org.mgroko.backend.notificaciones.mapper;

import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.notificaciones.dto.NotificacionResponse;

public final class NotificacionMapper {

    private static final String ENTIDAD_ACTIVIDAD = "ACTIVIDAD";
    private static final String ENTIDAD_HITO = "HITO";
    private static final String ENTIDAD_INVITACION_GRAL = "INVITACION_GRAL";
    private static final String ENTIDAD_INVITACION_ACTIVIDAD = "INVITACION_ACTIVIDAD";

    private NotificacionMapper() {
    }

    public static NotificacionResponse toResponse(Notificacion notificacion, Long idProyecto) {
        String entidadTipo = notificacion.getEntidadTipo();
        Long entidadId = notificacion.getEntidadId();

        return new NotificacionResponse(
                notificacion.getIdNotificacion(),
                notificacion.getTipo(),
                NotificacionTitulos.tituloDe(notificacion.getTipo()),
                notificacion.getMensaje(),
                entidadTipo,
                entidadId,
                notificacion.getLeida(),
                notificacion.getFechaLeida(),
                notificacion.getOrigen(),
                notificacion.getFechaGenerada(),
                notificacion.getPerfilContexto() != null
                        ? notificacion.getPerfilContexto().getIdPerfil()
                        : null,
                idProyecto,
                ENTIDAD_ACTIVIDAD.equals(entidadTipo) ? entidadId : null,
                ENTIDAD_HITO.equals(entidadTipo) ? entidadId : null,
                esEntidadInvitacion(entidadTipo) ? entidadId : null);
    }

    private static boolean esEntidadInvitacion(String entidadTipo) {
        return ENTIDAD_INVITACION_GRAL.equals(entidadTipo)
                || ENTIDAD_INVITACION_ACTIVIDAD.equals(entidadTipo);
    }
}
