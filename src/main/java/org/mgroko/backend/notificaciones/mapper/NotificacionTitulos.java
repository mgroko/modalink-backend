package org.mgroko.backend.notificaciones.mapper;

import java.util.Map;

/**
 * Catálogo de títulos legibles por tipo de notificación.
 *
 * <p>El título se deriva del {@code tipo} para que también esté disponible en
 * notificaciones históricas. Los nombres coinciden con los cargados en
 * {@code notificacion_tipo}. Para un tipo desconocido se devuelve {@code null}
 * y el frontend puede usar su fallback.</p>
 */
public final class NotificacionTitulos {

    private static final Map<String, String> TITULOS = Map.ofEntries(
            Map.entry("PROYECTO_CANCELADO", "Proyecto cancelado"),
            Map.entry("PROYECTO_CONFIRMADO", "Proyecto confirmado"),
            Map.entry("PROYECTO_PUBLICADO", "Proyecto publicado"),
            Map.entry("PROYECTO_FINALIZADO", "Proyecto finalizado"),
            Map.entry("CALIFICACION_SOLICITADA", "Calificación solicitada"),
            Map.entry("PROYECTO_FECHA_ENTREGA_MODIFICADA", "Fecha de entrega modificada"),
            Map.entry("ACTIVIDAD_MODIFICADA", "Actividad modificada"),
            Map.entry("ACTIVIDAD_ELIMINADA", "Actividad eliminada"),
            Map.entry("ACTIVIDAD_DESASIGNADA", "Actividad desasignada"),
            Map.entry("ACTIVIDAD_ASIGNADA", "Actividad asignada"),
            Map.entry("POSTULACION_RECIBIDA", "Postulación recibida"),
            Map.entry("POSTULACION_ACEPTADA", "Postulación aceptada"),
            Map.entry("POSTULACION_RECHAZADA", "Postulación rechazada"),
            Map.entry("AJUSTE_HORARIO", "Ajuste horario"),
            Map.entry("SOBRECOMPROMISO", "Posible sobrecompromiso"),
            Map.entry("INVITACION_RECIBIDA", "Invitación recibida"),
            Map.entry("INVITACION_ACEPTADA", "Invitación aceptada"),
            Map.entry("INVITACION_RECHAZADA", "Invitación rechazada"),
            Map.entry("HITO_CUMPLIDO", "Hito cumplido"),
            Map.entry("PERFIL_REPORTADO", "Perfil reportado"),
            Map.entry("PUBLICACION_REPORTADA", "Publicación reportada"),
            Map.entry("PUBLICACION_OCULTADA", "Publicación ocultada"),
            Map.entry("PUBLICACION_REACTIVADA", "Publicación reactivada"),
            Map.entry("HABILIDAD_MODIFICADA", "Habilidad modificada"),
            Map.entry("HABILIDAD_ELIMINADA", "Habilidad eliminada"),
            Map.entry("INTEGRANTE_ELIMINADO", "Integrante eliminado"),
            Map.entry("INTEGRANTE_BAJA_VOLUNTARIA", "Baja voluntaria de integrante"),
            Map.entry("REEMPLAZO_CANDIDATOS_DISPONIBLES", "Candidatos de reemplazo"),
            Map.entry("REEMPLAZO_SIN_CANDIDATOS", "Reemplazo sin candidatos"),
            Map.entry("REEMPLAZO_INVITACION_RECIBIDA", "Invitación de reemplazo"),
            Map.entry("REEMPLAZO_CANDIDATO_RECHAZO", "Candidato rechazó la invitación"),
            Map.entry("REEMPLAZO_ACEPTADO_PENDIENTE_APROBACION", "Incorporación pendiente de aprobación"),
            Map.entry("REEMPLAZO_INCORPORACION_APROBADA", "Incorporación aprobada"),
            Map.entry("REEMPLAZO_INCORPORACION_RECHAZADA", "Incorporación rechazada"),
            Map.entry("REEMPLAZO_CANCELADO", "Reemplazo cancelado"),
            Map.entry("SOLICITUD_CONEXION_RECIBIDA", "Solicitud de conexión recibida"),
            Map.entry("SOLICITUD_CONEXION_ACEPTADA", "Solicitud de conexión aceptada"),
            Map.entry("SOLICITUD_CONEXION_RECHAZADA", "Solicitud de conexión rechazada"),
            Map.entry("CONEXION_ELIMINADA", "Conexión eliminada"),
            Map.entry("SOLICITUD_COLABORACION_RECIBIDA", "Solicitud de colaboración recibida"),
            Map.entry("SOLICITUD_COLABORACION_ACEPTADA", "Solicitud de colaboración aceptada"),
            Map.entry("SOLICITUD_COLABORACION_RECHAZADA", "Solicitud de colaboración rechazada"),
            Map.entry("PUBLICACION_MEGUSTA", "Nueva interacción en publicación"),
            Map.entry("PUBLICACION_COMENTARIO", "Nuevo comentario en publicación"),
            Map.entry("MENSAJE_PRIVADO_RECIBIDO", "Mensaje privado recibido"),
            Map.entry("MENSAJE_GRUPAL_RECIBIDO", "Mensaje grupal recibido"));

    private NotificacionTitulos() {
    }

    public static String tituloDe(String tipo) {
        return tipo == null ? null : TITULOS.get(tipo);
    }
}
