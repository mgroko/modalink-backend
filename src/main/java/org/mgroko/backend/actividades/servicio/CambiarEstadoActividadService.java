package org.mgroko.backend.actividades.servicio;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.mgroko.backend.actividades.dto.ActividadResponse;
import org.mgroko.backend.actividades.evento.ActividadModificadaEvent;
import org.mgroko.backend.actividades.exception.ActividadNoEncontradaException;
import org.mgroko.backend.actividades.exception.ActividadSinFechaInicioException;
import org.mgroko.backend.actividades.exception.EstadoProyectoNoPermiteModificarActividadesException;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.servicio.MotivoReprogramacion;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.proyectos.servicio.ReprogramarProyectoService;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cambia únicamente el estado de una actividad.
 *
 * <p>No procesa requerimientos ni datos estructurales. Si la actividad se
 * cancela en un proyecto confirmado, reconecta sus sucesoras y dispara una
 * reprogramación global para que el cronograma persistido siga siendo
 * consistente con la disponibilidad de los miembros.</p>
 */
@Service
public class CambiarEstadoActividadService {

    private final PerfilRepository perfilRepository;
    private final ProyectoRepository proyectoRepository;
    private final PlanificacionRepository planificacionRepository;
    private final ActividadRepository actividadRepository;
    private final AsignacionActividadRepository asignacionActividadRepository;
    private final ProyectoSecurityService proyectoSecurityService;
    private final ObtenerActividadService obtenerActividadService;
    private final ReprogramarProyectoService reprogramarProyectoService;
    private final ApplicationEventPublisher eventPublisher;

    public CambiarEstadoActividadService(
            PerfilRepository perfilRepository,
            ProyectoRepository proyectoRepository,
            PlanificacionRepository planificacionRepository,
            ActividadRepository actividadRepository,
            AsignacionActividadRepository asignacionActividadRepository,
            ProyectoSecurityService proyectoSecurityService,
            ObtenerActividadService obtenerActividadService,
            ReprogramarProyectoService reprogramarProyectoService,
            ApplicationEventPublisher eventPublisher) {
        this.perfilRepository = perfilRepository;
        this.proyectoRepository = proyectoRepository;
        this.planificacionRepository = planificacionRepository;
        this.actividadRepository = actividadRepository;
        this.asignacionActividadRepository = asignacionActividadRepository;
        this.proyectoSecurityService = proyectoSecurityService;
        this.obtenerActividadService = obtenerActividadService;
        this.reprogramarProyectoService = reprogramarProyectoService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ActividadResponse cambiar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Long idActividad,
            EstadoActividad estadoNuevo) {

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfil = perfilRepository
                .findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() != EstadoPerfil.Activo) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }

        // Orden de locks acordado: proyecto y luego planificación. Reprogramar
        // toma los mismos locks y así se evita una inversión con otros flujos.
        Proyecto proyecto = proyectoRepository.findByIdParaGestion(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        Planificacion planificacion = planificacionRepository
                .findByProyectoIdProyectoForUpdate(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        validarEstadoProyecto(proyecto);
        proyectoSecurityService.validarPermiso(
                idProyecto,
                idPerfilActivo,
                ModificarActividadService.PERMISO_MODIFICAR_ACTIVIDAD);

        Actividad actividad = actividadRepository
                .findByIdActividadAndPlanificacionIdPlanificacion(
                        idActividad, planificacion.getIdPlanificacion())
                .orElseThrow(() -> new ActividadNoEncontradaException(idActividad));

        if (estadoNuevo == EstadoActividad.EN_CURSO
                || estadoNuevo == EstadoActividad.FINALIZADA) {
            List<Actividad> actividades = actividadRepository
                    .findConPredecesorasByPlanificacion(
                            planificacion.getIdPlanificacion());
            Actividad actividadConPredecesoras = actividades.stream()
                    .filter(candidata -> candidata.getIdActividad().equals(idActividad))
                    .findFirst()
                    .orElse(actividad);
            ActividadEstadoValidator.validar(
                    actividadConPredecesoras, estadoNuevo);
        } else {
            ActividadEstadoValidator.validar(
                    actividad.getEstado(), estadoNuevo);
        }

        if ((estadoNuevo == EstadoActividad.EN_CURSO
                || estadoNuevo == EstadoActividad.FINALIZADA)
                && actividad.getFechaHoraInicio() == null) {
            throw new ActividadSinFechaInicioException(
                    idActividad, estadoNuevo.name());
        }

        actividad.setEstado(estadoNuevo);
        actividadRepository.saveAndFlush(actividad);

        if (estadoNuevo == EstadoActividad.CANCELADA) {
            List<Actividad> actividades = actividadRepository
                    .findConPredecesorasByPlanificacion(
                            planificacion.getIdPlanificacion());
            ActividadDependencias.reconectarSucesoras(
                    actividadRepository, actividad, actividades);

            if (proyecto.getEstado() == EstadoProyecto.CONFIRMADO) {
                reprogramarProyectoService.reprogramar(
                        idProyecto,
                        MotivoReprogramacion.CANCELACION_ACTIVIDAD,
                        null);
            }
        }

        publicarActividadModificada(actividad, proyecto);

        return obtenerActividadService.obtener(
                idUsuario, idPerfilActivo, idProyecto, idActividad);
    }

    private void validarEstadoProyecto(Proyecto proyecto) {
        EstadoProyecto estado = proyecto.getEstado();
        if (estado != EstadoProyecto.BORRADOR
                && estado != EstadoProyecto.PUBLICADO
                && estado != EstadoProyecto.CONFIRMADO) {
            throw new EstadoProyectoNoPermiteModificarActividadesException(
                    estado.name());
        }
    }

    private void publicarActividadModificada(
            Actividad actividad,
            Proyecto proyecto) {

        List<AsignacionActividad> asignaciones = asignacionActividadRepository
                .findActivasByActividadId(actividad.getIdActividad());
        if (asignaciones.isEmpty()) {
            return;
        }

        Set<Long> idsPerfiles = new LinkedHashSet<>();
        for (AsignacionActividad asignacion : asignaciones) {
            idsPerfiles.add(asignacion.getMiembro().getPerfil().getIdPerfil());
        }

        eventPublisher.publishEvent(new ActividadModificadaEvent(
                actividad.getIdActividad(),
                proyecto.getIdProyecto(),
                actividad.getNombre(),
                proyecto.getNombre(),
                List.copyOf(idsPerfiles)));
    }
}
