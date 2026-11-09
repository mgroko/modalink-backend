package org.mgroko.backend.notificaciones.mapper;

import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.notificaciones.dto.NotificacionResponse;

public final class NotificacionMapper {

    private static final String ENTIDAD_ACTIVIDAD = "ACTIVIDAD";
    private static final String ENTIDAD_HITO = "HITO";
    private static final String ENTIDAD_INVITACION_GRAL = "INVITACION_GRAL";
    private static final String ENTIDAD_INVITACION_ACTIVIDAD = "INVITACION_ACTIVIDAD";
    private static final String ENTIDAD_CONEXION = "CONEXION";
    private static final String TIPO_CALIFICACION_SOLICITADA =
            "CALIFICACION_SOLICITADA";
    private static final String TIPO_SOLICITUD_CONEXION_RECIBIDA =
            "SOLICITUD_CONEXION_RECIBIDA";
    private static final String TIPO_SOLICITUD_CONEXION_ACEPTADA =
            "SOLICITUD_CONEXION_ACEPTADA";
    private static final String TIPO_SOLICITUD_CONEXION_RECHAZADA =
            "SOLICITUD_CONEXION_RECHAZADA";
    private static final String TIPO_CONEXION_ELIMINADA =
            "CONEXION_ELIMINADA";

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
                esEntidadInvitacion(entidadTipo) ? entidadId : null,
                construirLink(notificacion, idProyecto));
    }

    private static boolean esEntidadInvitacion(String entidadTipo) {
        return ENTIDAD_INVITACION_GRAL.equals(entidadTipo)
                || ENTIDAD_INVITACION_ACTIVIDAD.equals(entidadTipo);
    }

    private static String construirLink(Notificacion notificacion, Long idProyecto) {
        String tipo = notificacion.getTipo();

        if (TIPO_CALIFICACION_SOLICITADA.equals(tipo) && idProyecto != null) {
            return "/proyectos/" + idProyecto + "/calificaciones";
        }

        if (tipo == null) {
            return null;
        }

        return switch (tipo) {
            case TIPO_SOLICITUD_CONEXION_RECIBIDA ->
                    "/solicitudes-conexion?direccion=RECIBIDA";
            case TIPO_SOLICITUD_CONEXION_ACEPTADA ->
                    ENTIDAD_CONEXION.equals(notificacion.getEntidadTipo())
                            && notificacion.getEntidadId() != null
                            ? "/red-contactos?idConexion=" + notificacion.getEntidadId()
                            : "/red-contactos";
            case TIPO_SOLICITUD_CONEXION_RECHAZADA ->
                    "/solicitudes-conexion?direccion=ENVIADA&estado=RECHAZADA";
            case TIPO_CONEXION_ELIMINADA -> "/red-contactos";
            default -> null;
        };
    }
}
