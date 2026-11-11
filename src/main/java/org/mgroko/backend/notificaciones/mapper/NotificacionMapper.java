package org.mgroko.backend.notificaciones.mapper;

import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.notificaciones.dto.NotificacionResponse;

public final class NotificacionMapper {

    private static final String ENTIDAD_ACTIVIDAD = "ACTIVIDAD";
    private static final String ENTIDAD_HITO = "HITO";
    private static final String ENTIDAD_INVITACION_GRAL = "INVITACION_GRAL";
    private static final String ENTIDAD_INVITACION_ACTIVIDAD = "INVITACION_ACTIVIDAD";
    private static final String ENTIDAD_CONEXION = "CONEXION";
    private static final String ENTIDAD_CONVERSACION = "CONVERSACION";
    private static final String ENTIDAD_PUBLICACION = "PUBLICACION";
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
    private static final String TIPO_MENSAJE_PRIVADO_RECIBIDO =
            "MENSAJE_PRIVADO_RECIBIDO";
    private static final String TIPO_MENSAJE_GRUPAL_RECIBIDO =
            "MENSAJE_GRUPAL_RECIBIDO";
    private static final String TIPO_SOLICITUD_COLABORACION_RECIBIDA =
            "SOLICITUD_COLABORACION_RECIBIDA";

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
                    "/mi-red?tab=recibidas";
            case TIPO_SOLICITUD_CONEXION_ACEPTADA ->
                    ENTIDAD_CONEXION.equals(notificacion.getEntidadTipo())
                            && notificacion.getEntidadId() != null
                            ? "/mi-red?tab=conexiones&idConexion="
                                    + notificacion.getEntidadId()
                            : "/mi-red?tab=conexiones";
            case TIPO_SOLICITUD_CONEXION_RECHAZADA ->
                    "/mi-red?tab=enviadas&estado=RECHAZADA";
            case TIPO_CONEXION_ELIMINADA -> "/mi-red?tab=conexiones";
            case TIPO_MENSAJE_PRIVADO_RECIBIDO ->
                    ENTIDAD_CONVERSACION.equals(notificacion.getEntidadTipo())
                            && notificacion.getEntidadId() != null
                            ? "/mensajes?conversacion=" + notificacion.getEntidadId()
                            : "/mensajes";
            case TIPO_MENSAJE_GRUPAL_RECIBIDO ->
                    idProyecto != null
                            ? "/proyectos/" + idProyecto + "/chat"
                            : null;
            case TIPO_SOLICITUD_COLABORACION_RECIBIDA ->
                    ENTIDAD_PUBLICACION.equals(notificacion.getEntidadTipo())
                            && notificacion.getEntidadId() != null
                            ? "/publicaciones/" + notificacion.getEntidadId()
                            : null;
            default -> null;
        };
    }
}
