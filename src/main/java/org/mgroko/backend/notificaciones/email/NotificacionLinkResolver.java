package org.mgroko.backend.notificaciones.email;

import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.springframework.stereotype.Component;

/**
 * Resuelve la ruta relativa del frontend a la que debe llevar el email.
 *
 * <p>Se mantiene una única tabla de rutas para los tipos que se envían por
 * email. Si un tipo no tiene un destino navegable, se usa la bandeja de
 * notificaciones como fallback.</p>
 */
@Component
public class NotificacionLinkResolver {

    public static final String FALLBACK = "/notificaciones";

    public String resolver(Notificacion notificacion, Long idProyectoResuelto) {
        if (notificacion == null || notificacion.getTipo() == null) {
            return FALLBACK;
        }

        String tipo = notificacion.getTipo();
        Long entidadId = notificacion.getEntidadId();
        Long idProyecto = idProyectoResuelto != null
                ? idProyectoResuelto
                : notificacion.getIdProyecto();

        return switch (tipo) {
            case NotificacionService.TIPO_PROYECTO_CANCELADO,
                 NotificacionService.TIPO_PROYECTO_CONFIRMADO,
                 NotificacionService.TIPO_PROYECTO_FINALIZADO,
                 NotificacionService.TIPO_PROYECTO_PUBLICADO,
                 NotificacionService.TIPO_PROYECTO_FECHA_ENTREGA_MODIFICADA,
                 NotificacionService.TIPO_CALIFICACION_SOLICITADA ->
                    rutaProyecto(idProyecto, tipo.equals(NotificacionService.TIPO_CALIFICACION_SOLICITADA)
                            ? "/calificaciones"
                            : "");

            case NotificacionService.TIPO_ACTIVIDAD_ASIGNADA,
                 NotificacionService.TIPO_ACTIVIDAD_MODIFICADA,
                 NotificacionService.TIPO_AJUSTE_HORARIO,
                 NotificacionService.TIPO_SOBRECOMPROMISO ->
                    rutaActividad(idProyecto, entidadId);

            case NotificacionService.TIPO_ACTIVIDAD_ELIMINADA,
                 NotificacionService.TIPO_ACTIVIDAD_DESASIGNADA ->
                    rutaProyecto(idProyecto, "");

            case NotificacionService.TIPO_POSTULACION_RECIBIDA,
                 NotificacionService.TIPO_POSTULACION_ACEPTADA,
                 NotificacionService.TIPO_POSTULACION_RECHAZADA ->
                    rutaProyecto(idProyecto, "/postulaciones");

            case NotificacionService.TIPO_INVITACION_RECIBIDA,
                 NotificacionService.TIPO_INVITACION_ACEPTADA,
                 NotificacionService.TIPO_INVITACION_RECHAZADA ->
                    "/solicitudes";

            case NotificacionService.TIPO_REEMPLAZO_INVITACION_RECIBIDA,
                 NotificacionService.TIPO_REEMPLAZO_CANDIDATO_RECHAZO,
                 NotificacionService.TIPO_REEMPLAZO_INCORPORACION_RECHAZADA,
                 NotificacionService.TIPO_REEMPLAZO_CANCELADO ->
                    "/solicitudes?seccion=reemplazos";

            case NotificacionService.TIPO_REEMPLAZO_CANDIDATOS_DISPONIBLES,
                 NotificacionService.TIPO_REEMPLAZO_SIN_CANDIDATOS,
                 NotificacionService.TIPO_REEMPLAZO_ACEPTADO_PENDIENTE_APROBACION ->
                    rutaProyectoQuery(idProyecto, "reemplazos");

            case NotificacionService.TIPO_REEMPLAZO_INCORPORACION_APROBADA ->
                    rutaProyecto(idProyecto, "");

            case NotificacionService.TIPO_CONTRATO_PENDIENTE_FIRMA,
                 NotificacionService.TIPO_CONTRATO_MODIFICADO,
                 NotificacionService.TIPO_CONTRATO_FIRMADO,
                 NotificacionService.TIPO_CONTRATO_ACTIVO,
                 NotificacionService.TIPO_CONTRATO_RECHAZADO,
                 NotificacionService.TIPO_CONTRATO_CANCELADO,
                 NotificacionService.TIPO_CONTRATO_ELIMINADO ->
                    rutaContrato(idProyecto, entidadId);

            case NotificacionService.TIPO_SOLICITUD_COLABORACION_RECIBIDA,
                 NotificacionService.TIPO_SOLICITUD_COLABORACION_ACEPTADA,
                 NotificacionService.TIPO_SOLICITUD_COLABORACION_RECHAZADA ->
                    rutaEntidad("/publicaciones/", entidadId);

            case NotificacionService.TIPO_SOLICITUD_CONEXION_RECIBIDA ->
                    "/mi-red?tab=recibidas";

            case NotificacionService.TIPO_PERFIL_REPORTADO ->
                    rutaEntidad("/admin/reportes-perfil/", entidadId);

            case NotificacionService.TIPO_PUBLICACION_REPORTADA ->
                    rutaEntidad("/admin/reportes-publicacion/", entidadId);

            case NotificacionService.TIPO_INTEGRANTE_ELIMINADO,
                 NotificacionService.TIPO_INTEGRANTE_BAJA_VOLUNTARIA ->
                    FALLBACK;

            default -> FALLBACK;
        };
    }

    private String rutaProyecto(Long idProyecto, String sufijo) {
        return idProyecto == null ? FALLBACK : "/proyectos/" + idProyecto + sufijo;
    }

    private String rutaProyectoQuery(Long idProyecto, String tab) {
        return idProyecto == null ? FALLBACK : "/proyectos/" + idProyecto + "?tab=" + tab;
    }

    private String rutaActividad(Long idProyecto, Long idActividad) {
        if (idProyecto == null || idActividad == null) {
            return FALLBACK;
        }
        return "/proyectos/" + idProyecto + "/actividades/" + idActividad;
    }

    private String rutaContrato(Long idProyecto, Long idContrato) {
        if (idProyecto == null || idContrato == null) {
            return FALLBACK;
        }
        return "/proyectos/" + idProyecto + "/contratos/" + idContrato;
    }

    private String rutaEntidad(String prefijo, Long id) {
        return id == null ? FALLBACK : prefijo + id;
    }
}
