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
            Map.entry("PROYECTO_FECHA_ENTREGA_MODIFICADA", "Fecha de entrega modificada"),
            Map.entry("ACTIVIDAD_MODIFICADA", "Actividad modificada"),
            Map.entry("ACTIVIDAD_ELIMINADA", "Actividad eliminada"),
            Map.entry("ACTIVIDAD_ASIGNADA", "Actividad asignada"),
            Map.entry("POSTULACION_RECIBIDA", "Postulación recibida"),
            Map.entry("POSTULACION_ACEPTADA", "Postulación aceptada"),
            Map.entry("POSTULACION_RECHAZADA", "Postulación rechazada"),
            Map.entry("AJUSTE_HORARIO", "Ajuste horario"),
            Map.entry("SOBRECOMPROMISO", "Posible sobrecompromiso"),
            Map.entry("INVITACION_ACEPTADA", "Invitación aceptada"),
            Map.entry("INVITACION_RECHAZADA", "Invitación rechazada"),
            Map.entry("HITO_CUMPLIDO", "Hito cumplido"));

    private NotificacionTitulos() {
    }

    public static String tituloDe(String tipo) {
        return tipo == null ? null : TITULOS.get(tipo);
    }
}
