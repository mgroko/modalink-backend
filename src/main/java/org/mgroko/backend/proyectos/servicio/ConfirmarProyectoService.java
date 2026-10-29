package org.mgroko.backend.proyectos.servicio;

import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.actividades.dto.CronogramaResponse;
import org.mgroko.backend.actividades.servicio.ObtenerCronogramaService;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.OrigenInicio;
import org.mgroko.backend.proyectos.dto.BloqueoConfirmacionResponse;
import org.mgroko.backend.proyectos.dto.ConfirmarProyectoResponse;
import org.mgroko.backend.proyectos.evento.ProyectoConfirmadoEvent;
import org.mgroko.backend.proyectos.exception.ProyectoNoConfirmableException;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-60: confirma un proyecto en estado Publicado.
 *
 * <p>La operación es atómica: valida requerimientos y asignaciones, fija la
 * fecha de inicio con el reloj del backend, calcula el CPM, coloca
 * todas las actividades y persiste sus horarios. Cualquier fallo revierte el
 * estado y los horarios.</p>
 */
@Service
public class ConfirmarProyectoService {

    private final ContextoConfirmacionProyectoService contextoService;
    private final PlanificadorRecursosService planificadorRecursosService;
    private final ProyectoRepository proyectoRepository;
    private final ActividadRepository actividadRepository;
    private final ObtenerProyectoService obtenerProyectoService;
    private final ObtenerCronogramaService obtenerCronogramaService;
    private final ApplicationEventPublisher eventPublisher;

    public ConfirmarProyectoService(
            ContextoConfirmacionProyectoService contextoService,
            PlanificadorRecursosService planificadorRecursosService,
            ProyectoRepository proyectoRepository,
            ActividadRepository actividadRepository,
            ObtenerProyectoService obtenerProyectoService,
            ObtenerCronogramaService obtenerCronogramaService,
            ApplicationEventPublisher eventPublisher) {
        this.contextoService = contextoService;
        this.planificadorRecursosService = planificadorRecursosService;
        this.proyectoRepository = proyectoRepository;
        this.actividadRepository = actividadRepository;
        this.obtenerProyectoService = obtenerProyectoService;
        this.obtenerCronogramaService = obtenerCronogramaService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ConfirmarProyectoResponse confirmar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto) {

        ContextoConfirmacionProyectoService.Contexto contexto =
                contextoService.cargar(idUsuario, idPerfilActivo, idProyecto, true);

        if (!contexto.bloqueos().isEmpty()) {
            BloqueoConfirmacionResponse primero = contexto.bloqueos().get(0);
            throw new ProyectoNoConfirmableException(
                    primero.codigo(),
                    primero.mensaje(),
                    contexto.bloqueos());
        }

        LocalDateTime ahora = contexto.fechaInicioPropuesta();
        Proyecto proyecto = contexto.proyecto();
        Planificacion planificacion = contexto.planificacion();
        List<Actividad> actividades = contexto.actividades();

        // Las fechas de una actividad PENDIENTE en un proyecto no confirmado
        // son proyecciones legadas: la confirmación las reemplaza por el plan
        // calculado. No se persisten hasta el final de la transacción.
        for (Actividad actividad : actividades) {
            if (actividad.getEstado() == EstadoActividad.PENDIENTE) {
                actividad.setFechaHoraInicio(null);
                actividad.setOrigenInicio(null);
            }
        }

        proyecto.setEstado(EstadoProyecto.CONFIRMADO);
        proyecto.setFechaInicio(ahora);
        proyectoRepository.saveAndFlush(proyecto);

        PlanificadorRecursosService.Resultado plan = planificadorRecursosService
                .planificar(planificacion, actividades, ahora, true);

        for (Actividad actividad : actividades) {
            if (actividad.getEstado() == EstadoActividad.CANCELADA) {
                continue;
            }

            LocalDateTime inicio = plan.inicios().get(actividad.getIdActividad());
            if (inicio == null) {
                throw new ProyectoNoConfirmableException(
                        "DATOS_INCONSISTENTES",
                        "No se pudo asignar un horario a la actividad '"
                                + actividad.getNombre() + "'.",
                        List.of(new BloqueoConfirmacionResponse(
                                "DATOS_INCONSISTENTES",
                                "Actividad sin horario luego de la planificación.",
                                "ACTIVIDAD",
                                actividad.getIdActividad())));
            }

            actividad.setFechaHoraInicio(inicio);
            if (actividad.getEstado() == EstadoActividad.PENDIENTE) {
                actividad.setOrigenInicio(OrigenInicio.CONFIRMADA);
            } else {
                actividad.setOrigenInicio(OrigenInicio.REAL);
            }
        }
        actividadRepository.saveAll(actividades);
        actividadRepository.flush();

        eventPublisher.publishEvent(new ProyectoConfirmadoEvent(
                proyecto.getIdProyecto(),
                proyecto.getNombre(),
                ahora));

        CronogramaResponse cronograma = obtenerCronogramaService.obtener(
                idUsuario, idPerfilActivo, idProyecto);

        return new ConfirmarProyectoResponse(
                obtenerProyectoService.obtener(idUsuario, idPerfilActivo, idProyecto),
                ahora,
                cronograma);
    }
}
