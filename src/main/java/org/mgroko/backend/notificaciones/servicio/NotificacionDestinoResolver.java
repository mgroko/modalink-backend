package org.mgroko.backend.notificaciones.servicio;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.repositorio.PostulacionActividadRepository;
import org.mgroko.backend.repositorio.PostulacionGralRepository;
import org.mgroko.backend.repositorio.proyeccion.ProyectoDeEntidad;
import org.springframework.stereotype.Component;

/**
 * Resuelve el proyecto asociado a una notificación a partir de su referencia
 * polimórfica ({@code entidadTipo} + {@code entidadId}).
 *
 * <p>El proyecto no se persiste en la notificación: se deriva de la entidad
 * referenciada. De esta forma no hay una segunda fuente de verdad y las
 * notificaciones históricas también obtienen el dato.</p>
 */
@Component
public class NotificacionDestinoResolver {

    public static final String ENTIDAD_PROYECTO = "PROYECTO";
    public static final String ENTIDAD_POSTULACION_GRAL = "POSTULACION_GRAL";
    public static final String ENTIDAD_POSTULACION_ACTIVIDAD = "POSTULACION_ACTIVIDAD";

    private final PostulacionGralRepository postulacionGralRepository;
    private final PostulacionActividadRepository postulacionActividadRepository;

    public NotificacionDestinoResolver(
            PostulacionGralRepository postulacionGralRepository,
            PostulacionActividadRepository postulacionActividadRepository) {
        this.postulacionGralRepository = postulacionGralRepository;
        this.postulacionActividadRepository = postulacionActividadRepository;
    }

    /**
     * Devuelve un mapa {@code idNotificacion -> idProyecto} para aquellas
     * notificaciones cuyo proyecto se puede resolver. Las que no tienen
     * destino conocido se omiten.
     */
    public Map<Long, Long> resolverProyectos(Collection<Notificacion> notificaciones) {
        if (notificaciones == null || notificaciones.isEmpty()) {
            return Map.of();
        }

        Set<Long> idsPostulacionesGral = new HashSet<>();
        Set<Long> idsPostulacionesActividad = new HashSet<>();
        Map<Long, Long> proyectos = new HashMap<>();

        for (Notificacion notificacion : notificaciones) {
            if (notificacion == null
                    || notificacion.getIdNotificacion() == null
                    || notificacion.getEntidadId() == null) {
                continue;
            }

            if (ENTIDAD_PROYECTO.equals(notificacion.getEntidadTipo())) {
                proyectos.put(notificacion.getIdNotificacion(), notificacion.getEntidadId());
            } else if (ENTIDAD_POSTULACION_GRAL.equals(notificacion.getEntidadTipo())) {
                idsPostulacionesGral.add(notificacion.getEntidadId());
            } else if (ENTIDAD_POSTULACION_ACTIVIDAD.equals(notificacion.getEntidadTipo())) {
                idsPostulacionesActividad.add(notificacion.getEntidadId());
            }
        }

        Map<Long, Long> proyectoPorPostulacionGral = aMapa(
                idsPostulacionesGral.isEmpty()
                        ? List.of()
                        : postulacionGralRepository.findProyectoPorIds(idsPostulacionesGral));
        Map<Long, Long> proyectoPorPostulacionActividad = aMapa(
                idsPostulacionesActividad.isEmpty()
                        ? List.of()
                        : postulacionActividadRepository.findProyectoPorIds(idsPostulacionesActividad));

        for (Notificacion notificacion : notificaciones) {
            if (notificacion == null
                    || notificacion.getIdNotificacion() == null
                    || proyectos.containsKey(notificacion.getIdNotificacion())) {
                continue;
            }

            Long idProyecto = null;
            if (ENTIDAD_POSTULACION_GRAL.equals(notificacion.getEntidadTipo())) {
                idProyecto = proyectoPorPostulacionGral.get(notificacion.getEntidadId());
            } else if (ENTIDAD_POSTULACION_ACTIVIDAD.equals(notificacion.getEntidadTipo())) {
                idProyecto = proyectoPorPostulacionActividad.get(notificacion.getEntidadId());
            }

            if (idProyecto != null) {
                proyectos.put(notificacion.getIdNotificacion(), idProyecto);
            }
        }

        return proyectos;
    }

    /**
     * Resuelve el proyecto de una única notificación.
     */
    public Long resolverProyecto(Notificacion notificacion) {
        if (notificacion == null || notificacion.getIdNotificacion() == null) {
            return null;
        }
        return resolverProyectos(List.of(notificacion)).get(notificacion.getIdNotificacion());
    }

    private Map<Long, Long> aMapa(Collection<ProyectoDeEntidad> filas) {
        Map<Long, Long> mapa = new HashMap<>();
        for (ProyectoDeEntidad fila : filas) {
            if (fila.getEntidadId() != null && fila.getIdProyecto() != null) {
                mapa.put(fila.getEntidadId(), fila.getIdProyecto());
            }
        }
        return mapa;
    }
}
