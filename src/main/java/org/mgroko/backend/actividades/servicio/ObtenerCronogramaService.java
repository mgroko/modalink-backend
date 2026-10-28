package org.mgroko.backend.actividades.servicio;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.mgroko.backend.actividades.dto.ActividadCronogramaResponse;
import org.mgroko.backend.actividades.dto.ActividadVinculadaResponse;
import org.mgroko.backend.actividades.dto.CalculoCpm;
import org.mgroko.backend.actividades.dto.CronogramaResponse;
import org.mgroko.backend.actividades.dto.NodoCpm;
import org.mgroko.backend.actividades.dto.ResumenCronogramaResponse;
import org.mgroko.backend.actividades.mapper.ActividadMapper;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-33: cronograma de un proyecto.
 *
 * <p>Calcula el CPM sobre todas las actividades de la planificación excepto
 * {@code CANCELADA}. Devuelve el resumen de las tarjetas, las filas para el
 * Gantt y la tabla CPM, y la ruta crítica ordenada por inicio temprano.</p>
 */
@Service
public class ObtenerCronogramaService {

    private final ActividadRepository actividadRepository;
    private final PlanificacionRepository planificacionRepository;
    private final PerfilRepository perfilRepository;
    private final ProyectoSecurityService proyectoSecurityService;
    private final CronogramaService cronogramaService;

    public ObtenerCronogramaService(
            ActividadRepository actividadRepository,
            PlanificacionRepository planificacionRepository,
            PerfilRepository perfilRepository,
            ProyectoSecurityService proyectoSecurityService,
            CronogramaService cronogramaService) {
        this.actividadRepository = actividadRepository;
        this.planificacionRepository = planificacionRepository;
        this.perfilRepository = perfilRepository;
        this.proyectoSecurityService = proyectoSecurityService;
        this.cronogramaService = cronogramaService;
    }

    @Transactional(readOnly = true)
    public CronogramaResponse obtener(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto) {

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfil = perfilRepository
                .findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() != EstadoPerfil.Activo) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }

        Planificacion planificacion = planificacionRepository
                .findByProyectoIdProyecto(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        proyectoSecurityService.validarPermiso(
                idProyecto,
                idPerfilActivo,
                ProyectoSecurityService.PERMISO_VER_PROYECTO);

        List<Actividad> todas = actividadRepository
                .findConPredecesorasByPlanificacion(planificacion.getIdPlanificacion());

        CalculoCpm calculo = cronogramaService.calcularProyeccion(planificacion, todas);
        LocalDateTime inicio = cronogramaService.inicioEfectivo(planificacion);
        boolean tentativo = cronogramaService.esTentativa(planificacion);

        List<Long> rutaCritica = calculo.nodos().values().stream()
                .filter(NodoCpm::enRutaCritica)
                .sorted(Comparator
                        .comparingLong(NodoCpm::inicioTemprano)
                        .thenComparing(NodoCpm::idActividad))
                .map(NodoCpm::idActividad)
                .toList();

        ResumenCronogramaResponse resumen = new ResumenCronogramaResponse(
                inicio,
                inicio.plusMinutes(calculo.makespan()),
                planificacion.getFechaEntrega(),
                calculo.makespan(),
                calculo.margen(),
                tentativo,
                calculo.parcial(),
                cronogramaService.estadoDe(calculo).name(),
                calculo.viable(),
                calculo.nodos().size(),
                rutaCritica.size(),
                rutaCritica);

        List<ActividadCronogramaResponse> actividades = new ArrayList<>();
        for (Actividad actividad : OrdenActividades.topologico(todas)) {
            NodoCpm nodo = calculo.nodos().get(actividad.getIdActividad());
            if (nodo == null) {
                continue;
            }

            List<ActividadVinculadaResponse> predecesoras = actividad.getPredecesoras().stream()
                    .filter(predecesora -> predecesora.getEstado() != EstadoActividad.CANCELADA)
                    .sorted(Comparator.comparing(
                            Actividad::getNombre,
                            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                    .map(predecesora -> new ActividadVinculadaResponse(
                            predecesora.getIdActividad(), predecesora.getNombre()))
                    .toList();

            actividades.add(ActividadMapper.toCronogramaResponse(
                    actividad,
                    nodo,
                    inicio,
                    tentativo,
                    predecesoras));
        }

        return new CronogramaResponse(resumen, actividades, calculo.conflictos());
    }
}
