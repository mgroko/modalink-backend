package org.mgroko.backend.notificaciones.servicio;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.HitoRepository;
import org.mgroko.backend.repositorio.InvitacionActividadRepository;
import org.mgroko.backend.repositorio.InvitacionGralRepository;
import org.mgroko.backend.repositorio.PostulacionActividadRepository;
import org.mgroko.backend.repositorio.PostulacionGralRepository;
import org.mgroko.backend.repositorio.proyeccion.ProyectoDeEntidad;
import org.springframework.stereotype.Component;

/**
 * Resuelve el proyecto asociado a una notificación.
 *
 * <p>Primero usa el {@code idProyecto} capturado al generar la notificación.
 * Si no está presente, lo deriva de la referencia polimórfica
 * ({@code entidadTipo + entidadId}). De esta forma las notificaciones nuevas
 * conservan el proyecto aunque la entidad referenciada se elimine después, y
 * las históricas siguen funcionando mientras la entidad exista.</p>
 */
@Component
public class NotificacionDestinoResolver {

    public static final String ENTIDAD_PROYECTO = "PROYECTO";
    public static final String ENTIDAD_POSTULACION_GRAL = "POSTULACION_GRAL";
    public static final String ENTIDAD_POSTULACION_ACTIVIDAD = "POSTULACION_ACTIVIDAD";
    public static final String ENTIDAD_ACTIVIDAD = "ACTIVIDAD";
    public static final String ENTIDAD_HITO = "HITO";
    public static final String ENTIDAD_INVITACION_GRAL = "INVITACION_GRAL";
    public static final String ENTIDAD_INVITACION_ACTIVIDAD = "INVITACION_ACTIVIDAD";
    public static final String ENTIDAD_REPORTE_PERFIL = "REPORTE_PERFIL";

    private final PostulacionGralRepository postulacionGralRepository;
    private final PostulacionActividadRepository postulacionActividadRepository;
    private final ActividadRepository actividadRepository;
    private final InvitacionGralRepository invitacionGralRepository;
    private final InvitacionActividadRepository invitacionActividadRepository;
    private final HitoRepository hitoRepository;

    public NotificacionDestinoResolver(
            PostulacionGralRepository postulacionGralRepository,
            PostulacionActividadRepository postulacionActividadRepository,
            ActividadRepository actividadRepository,
            InvitacionGralRepository invitacionGralRepository,
            InvitacionActividadRepository invitacionActividadRepository,
            HitoRepository hitoRepository) {
        this.postulacionGralRepository = postulacionGralRepository;
        this.postulacionActividadRepository = postulacionActividadRepository;
        this.actividadRepository = actividadRepository;
        this.invitacionGralRepository = invitacionGralRepository;
        this.invitacionActividadRepository = invitacionActividadRepository;
        this.hitoRepository = hitoRepository;
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
        Set<Long> idsActividades = new HashSet<>();
        Set<Long> idsInvitacionesGral = new HashSet<>();
        Set<Long> idsInvitacionesActividad = new HashSet<>();
        Set<Long> idsHitos = new HashSet<>();
        Map<Long, Long> proyectos = new HashMap<>();

        for (Notificacion notificacion : notificaciones) {
            if (notificacion == null || notificacion.getIdNotificacion() == null) {
                continue;
            }

            if (notificacion.getIdProyecto() != null) {
                proyectos.put(
                        notificacion.getIdNotificacion(),
                        notificacion.getIdProyecto());
                continue;
            }

            if (notificacion.getEntidadId() == null
                    || notificacion.getEntidadTipo() == null) {
                continue;
            }

            switch (notificacion.getEntidadTipo()) {
                case ENTIDAD_PROYECTO -> proyectos.put(
                        notificacion.getIdNotificacion(),
                        notificacion.getEntidadId());
                case ENTIDAD_POSTULACION_GRAL -> idsPostulacionesGral.add(
                        notificacion.getEntidadId());
                case ENTIDAD_POSTULACION_ACTIVIDAD -> idsPostulacionesActividad.add(
                        notificacion.getEntidadId());
                case ENTIDAD_ACTIVIDAD -> idsActividades.add(
                        notificacion.getEntidadId());
                case ENTIDAD_INVITACION_GRAL -> idsInvitacionesGral.add(
                        notificacion.getEntidadId());
                case ENTIDAD_INVITACION_ACTIVIDAD -> idsInvitacionesActividad.add(
                        notificacion.getEntidadId());
                case ENTIDAD_HITO -> idsHitos.add(
                        notificacion.getEntidadId());
                default -> {
                    // Tipo sin proyecto navegable.
                }
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
        Map<Long, Long> proyectoPorActividad = aMapa(
                idsActividades.isEmpty()
                        ? List.of()
                        : actividadRepository.findProyectoPorIds(idsActividades));
        Map<Long, Long> proyectoPorInvitacionGral = aMapa(
                idsInvitacionesGral.isEmpty()
                        ? List.of()
                        : invitacionGralRepository.findProyectoPorIds(idsInvitacionesGral));
        Map<Long, Long> proyectoPorInvitacionActividad = aMapa(
                idsInvitacionesActividad.isEmpty()
                        ? List.of()
                        : invitacionActividadRepository.findProyectoPorIds(idsInvitacionesActividad));
        Map<Long, Long> proyectoPorHito = aMapa(
                idsHitos.isEmpty()
                        ? List.of()
                        : hitoRepository.findProyectoPorIds(idsHitos));

        for (Notificacion notificacion : notificaciones) {
            if (notificacion == null
                    || notificacion.getIdNotificacion() == null
                    || proyectos.containsKey(notificacion.getIdNotificacion())
                    || notificacion.getEntidadTipo() == null
                    || notificacion.getEntidadId() == null) {
                continue;
            }

            Long idProyecto = switch (notificacion.getEntidadTipo()) {
                case ENTIDAD_POSTULACION_GRAL ->
                    proyectoPorPostulacionGral.get(notificacion.getEntidadId());
                case ENTIDAD_POSTULACION_ACTIVIDAD ->
                    proyectoPorPostulacionActividad.get(notificacion.getEntidadId());
                case ENTIDAD_ACTIVIDAD ->
                    proyectoPorActividad.get(notificacion.getEntidadId());
                case ENTIDAD_INVITACION_GRAL ->
                    proyectoPorInvitacionGral.get(notificacion.getEntidadId());
                case ENTIDAD_INVITACION_ACTIVIDAD ->
                    proyectoPorInvitacionActividad.get(notificacion.getEntidadId());
                case ENTIDAD_HITO ->
                    proyectoPorHito.get(notificacion.getEntidadId());
                default -> null;
            };

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
        if (filas == null) {
            return mapa;
        }
        for (ProyectoDeEntidad fila : filas) {
            if (fila.getEntidadId() != null && fila.getIdProyecto() != null) {
                mapa.put(fila.getEntidadId(), fila.getIdProyecto());
            }
        }
        return mapa;
    }
}
