package org.mgroko.backend.actividades.servicio;

import java.util.List;

import org.mgroko.backend.actividades.dto.ActividadResponse;
import org.mgroko.backend.actividades.dto.RequerimientoActividadResponse;
import org.mgroko.backend.actividades.exception.ActividadNoEncontradaException;
import org.mgroko.backend.actividades.mapper.ActividadMapper;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.enums.EstadoCronograma;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.servicio.ProyectoVisibilidadService;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ObtenerActividadService {

    private final ActividadRepository actividadRepository;
    private final PlanificacionRepository planificacionRepository;
    private final PerfilRepository perfilRepository;
    private final ProyectoVisibilidadService proyectoVisibilidadService;
    private final RequerimientoActividadService requerimientoActividadService;
    private final AsignacionActividadRepository asignacionActividadRepository;
    private final CronogramaService cronogramaService;

    public ObtenerActividadService(
            ActividadRepository actividadRepository,
            PlanificacionRepository planificacionRepository,
            PerfilRepository perfilRepository,
            ProyectoVisibilidadService proyectoVisibilidadService,
            RequerimientoActividadService requerimientoActividadService,
            AsignacionActividadRepository asignacionActividadRepository,
            CronogramaService cronogramaService) {
        this.actividadRepository = actividadRepository;
        this.planificacionRepository = planificacionRepository;
        this.perfilRepository = perfilRepository;
        this.proyectoVisibilidadService = proyectoVisibilidadService;
        this.requerimientoActividadService = requerimientoActividadService;
        this.asignacionActividadRepository = asignacionActividadRepository;
        this.cronogramaService = cronogramaService;
    }

    @Transactional(readOnly = true)
    public ActividadResponse obtener(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Long idActividad) {

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfil = perfilRepository.findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() != EstadoPerfil.Activo) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }

        Planificacion planificacion = planificacionRepository
                .findByProyectoIdProyecto(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        proyectoVisibilidadService.validarAcceso(planificacion.getProyecto(), idPerfilActivo);

        Actividad actividad = actividadRepository
                .findByIdActividadAndPlanificacionIdPlanificacion(
                        idActividad, planificacion.getIdPlanificacion())
                .orElseThrow(() -> new ActividadNoEncontradaException(idActividad));

        List<RequerimientoActividadResponse> requerimientos =
                requerimientoActividadService.listarPorActividad(
                        idActividad, idPerfilActivo, planificacion.getProyecto());

        List<AsignacionActividad> asignaciones = asignacionActividadRepository
                .findActivasByActividadId(idActividad);

        var calculo = planificacion.getFechaEntrega() == null
                ? null
                : cronogramaService.calcularProyeccion(
                        planificacion,
                        actividadRepository.findConPredecesorasByPlanificacion(
                                planificacion.getIdPlanificacion()));
        EstadoCronograma estadoCronograma = calculo == null
                ? EstadoCronograma.SIN_FECHA_ENTREGA
                : cronogramaService.estadoDe(calculo);

        return ActividadMapper.toResponse(
                actividad,
                requerimientos,
                asignaciones.stream()
                        .map(ActividadMapper::toProfesionalAsignado)
                        .toList(),
                estadoCronograma,
                calculo,
                calculo == null ? null : cronogramaService.inicioEfectivo(planificacion),
                calculo == null || cronogramaService.esTentativa(planificacion));
    }
}
