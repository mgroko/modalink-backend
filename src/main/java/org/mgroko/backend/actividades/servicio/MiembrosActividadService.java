package org.mgroko.backend.actividades.servicio;

import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.actividades.dto.MiembroExclusivoResponse;
import org.mgroko.backend.actividades.exception.ActividadNoEncontradaException;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.RolProyectoUsuario;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MiembrosActividadService {

    private final AsignacionActividadRepository asignacionActividadRepository;
    private final ActividadRepository actividadRepository;
    private final PlanificacionRepository planificacionRepository;
    private final PerfilRepository perfilRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final ProyectoSecurityService proyectoSecurityService;

    public MiembrosActividadService(
            AsignacionActividadRepository asignacionActividadRepository,
            ActividadRepository actividadRepository,
            PlanificacionRepository planificacionRepository,
            PerfilRepository perfilRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            ProyectoSecurityService proyectoSecurityService) {
        this.asignacionActividadRepository = asignacionActividadRepository;
        this.actividadRepository = actividadRepository;
        this.planificacionRepository = planificacionRepository;
        this.perfilRepository = perfilRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.proyectoSecurityService = proyectoSecurityService;
    }

    /**
     * UC-51: miembros activos que participan únicamente de la actividad
     * indicada dentro del proyecto. Para cada uno se informa si puede
     * eliminarse del proyecto o si debe conservarse.
     */
    @Transactional(readOnly = true)
    public List<MiembroExclusivoResponse> listarExclusivos(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Long idActividad) {

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfilActor = perfilRepository
                .findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfilActor.getEstado() != EstadoPerfil.Activo) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }

        Planificacion planificacion = planificacionRepository
                .findByProyectoIdProyecto(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        Actividad actividad = actividadRepository
                .findByIdActividadAndPlanificacionIdPlanificacion(
                        idActividad, planificacion.getIdPlanificacion())
                .orElseThrow(() -> new ActividadNoEncontradaException(idActividad));

        proyectoSecurityService.validarPermiso(
                idProyecto,
                idPerfilActivo,
                EliminarActividadService.PERMISO_ELIMINAR_ACTIVIDAD);

        Proyecto proyecto = planificacion.getProyecto();
        List<AsignacionActividad> asignaciones = asignacionActividadRepository
                .findExclusivasByActividadIdAndProyecto(
                        actividad.getIdActividad(), idProyecto);

        List<MiembroExclusivoResponse> resultado = new ArrayList<>();
        for (AsignacionActividad asignacion : asignaciones) {
            resultado.add(toResponse(asignacion, proyecto, idPerfilActivo));
        }
        return resultado;
    }

    /**
     * Conserva al integrante en el proyecto quitando únicamente su asignación
     * a la actividad. Se mantiene para flujos individuales; la eliminación
     * atómica de la actividad no necesita invocarlo porque el borrado de la
     * actividad elimina todas sus asignaciones.
     */
    @Transactional
    public void guardarMiembro(Long idActividad, Long idPerfil) {
        asignacionActividadRepository.deleteByActividadIdAndPerfil(
                idActividad, idPerfil);
    }

    private MiembroExclusivoResponse toResponse(
            AsignacionActividad asignacion,
            Proyecto proyecto,
            Long idPerfilActivo) {

        MiembroProyecto miembro = asignacion.getMiembro();
        Perfil perfil = miembro != null ? miembro.getPerfil() : null;

        boolean esActor = perfil != null
                && perfil.getIdPerfil() != null
                && perfil.getIdPerfil().equals(idPerfilActivo);

        boolean esDirector = miembro != null
                && RolProyectoUsuario.desde(miembro.getRolProyecto())
                        == RolProyectoUsuario.DIRECTOR;

        boolean ultimoDirector = esDirector
                && miembroProyectoRepository.countOtrosDirectoresActivos(
                        proyecto.getIdProyecto(), miembro.getIdMiembro()) < 1;

        boolean proyectoBorrador = proyecto.getEstado() == EstadoProyecto.BORRADOR;

        String motivoNoEliminable = null;
        if (esActor) {
            motivoNoEliminable = "AUTOELIMINACION";
        } else if (ultimoDirector) {
            motivoNoEliminable = "ULTIMO_DIRECTOR";
        } else if (proyectoBorrador) {
            motivoNoEliminable = "PROYECTO_BORRADOR";
        }

        return new MiembroExclusivoResponse(
                asignacion.getIdAsignacionAct(),
                miembro != null ? miembro.getIdMiembro() : null,
                perfil != null ? perfil.getIdPerfil() : null,
                perfil != null && perfil.getUsuario() != null
                        ? perfil.getUsuario().getIdUsuario()
                        : null,
                perfil != null ? perfil.getNombreArtistico() : null,
                perfil != null && perfil.getProfesion() != null
                        ? perfil.getProfesion().getIdProfesion()
                        : null,
                perfil != null && perfil.getProfesion() != null
                        ? perfil.getProfesion().getNombre()
                        : null,
                motivoNoEliminable == null,
                motivoNoEliminable);
    }
}
