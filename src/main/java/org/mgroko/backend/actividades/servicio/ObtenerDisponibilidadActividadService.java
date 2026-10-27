package org.mgroko.backend.actividades.servicio;

import java.util.List;

import org.mgroko.backend.actividades.exception.ActividadCanceladaException;
import org.mgroko.backend.actividades.exception.ActividadNoEncontradaException;
import org.mgroko.backend.calendario.dto.ResultadoDisponibilidad;
import org.mgroko.backend.calendario.servicio.CalendarioService;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.servicio.ProyectoVisibilidadService;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta la disponibilidad de uno o más perfiles frente al rango factible de
 * una actividad (UC-26 y UC-70).
 *
 * <p>Sin {@code idsPerfil} evalúa el perfil activo del solicitante: es la
 * previsualización que un candidato usa antes de postularse a una actividad.
 * Con {@code idsPerfil} evalúa los perfiles indicados y, cuando son varios,
 * exige un hueco común entre todos ellos.</p>
 *
 * <p>La consulta es de solo lectura: no reserva ni asigna la actividad. Las
 * operaciones que sí modifican el estado (postularse, aceptar una postulación)
 * vuelven a validar la disponibilidad en el backend.</p>
 */
@Service
public class ObtenerDisponibilidadActividadService {

    private final ActividadRepository actividadRepository;
    private final PlanificacionRepository planificacionRepository;
    private final PerfilRepository perfilRepository;
    private final ProyectoVisibilidadService proyectoVisibilidadService;
    private final CalendarioService calendarioService;

    public ObtenerDisponibilidadActividadService(
            ActividadRepository actividadRepository,
            PlanificacionRepository planificacionRepository,
            PerfilRepository perfilRepository,
            ProyectoVisibilidadService proyectoVisibilidadService,
            CalendarioService calendarioService) {
        this.actividadRepository = actividadRepository;
        this.planificacionRepository = planificacionRepository;
        this.perfilRepository = perfilRepository;
        this.proyectoVisibilidadService = proyectoVisibilidadService;
        this.calendarioService = calendarioService;
    }

    @Transactional(readOnly = true)
    public ResultadoDisponibilidad obtener(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Long idActividad,
            List<Long> idsPerfil) {

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfilActivo = perfilRepository
                .findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfilActivo.getEstado() != EstadoPerfil.Activo) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }

        Planificacion planificacion = planificacionRepository
                .findByProyectoIdProyecto(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        Actividad actividad = actividadRepository
                .findByIdActividadAndPlanificacionIdPlanificacion(
                        idActividad, planificacion.getIdPlanificacion())
                .orElseThrow(() -> new ActividadNoEncontradaException(idActividad));

        if (actividad.getEstado() == EstadoActividad.CANCELADA) {
            throw new ActividadCanceladaException(idActividad);
        }

        Proyecto proyecto = planificacion.getProyecto();
        proyectoVisibilidadService.validarAcceso(proyecto, idPerfilActivo);

        List<Perfil> perfiles = resolverPerfiles(idsPerfil, perfilActivo);
        validarConsultaDeTerceros(idProyecto, idUsuario, idPerfilActivo, perfiles);

        List<Long> idsUsuarios = perfiles.stream()
                .map(perfil -> perfil.getUsuario().getIdUsuario())
                .distinct()
                .toList();

        return calendarioService.verificarDisponibilidadMiembros(idsUsuarios, actividad);
    }

    private List<Perfil> resolverPerfiles(List<Long> idsPerfil, Perfil perfilActivo) {
        if (idsPerfil == null || idsPerfil.isEmpty()) {
            return List.of(perfilActivo);
        }

        List<Perfil> perfiles = idsPerfil.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .map(this::perfilActivo)
                .toList();

        return perfiles.isEmpty() ? List.of(perfilActivo) : perfiles;
    }

    private Perfil perfilActivo(Long idPerfil) {
        Perfil perfil = perfilRepository.findById(idPerfil)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() != EstadoPerfil.Activo) {
            throw new PerfilEnBajaException(
                    "El perfil " + idPerfil + " no se encuentra activo.");
        }

        return perfil;
    }

    /**
     * Consultar la disponibilidad de terceros requiere ser miembro activo del
     * proyecto. La disponibilidad propia no lo requiere: el candidato todavía
     * no es miembro cuando previsualiza una actividad (UC-26).
     */
    private void validarConsultaDeTerceros(
            Long idProyecto,
            Long idUsuario,
            Long idPerfilActivo,
            List<Perfil> perfiles) {

        boolean consultaTerceros = perfiles.stream()
                .anyMatch(perfil -> !perfil.getUsuario().getIdUsuario().equals(idUsuario));

        if (consultaTerceros
                && !proyectoVisibilidadService.esMiembroActivo(idProyecto, idPerfilActivo)) {
            throw new AccesoDenegadoProyectoException(
                    "Solo los miembros activos del proyecto pueden consultar la disponibilidad de otros perfiles.");
        }
    }
}
