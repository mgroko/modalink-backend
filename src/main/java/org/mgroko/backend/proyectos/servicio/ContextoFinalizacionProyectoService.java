package org.mgroko.backend.proyectos.servicio;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.CalificacionPerfil;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.RolProyectoUsuario;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.dto.AdvertenciaFinalizacionResponse;
import org.mgroko.backend.proyectos.dto.BloqueoFinalizacionResponse;
import org.mgroko.backend.proyectos.dto.MiembroCalificableResponse;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.CalificacionPerfilRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga y valida el contexto común de UC-37.
 *
 * <p>Cuando {@code bloquear} es {@code true} toma locks pesimistas del
 * proyecto y de su planificación. El servicio de finalización lo usa con
 * lock; la previsualización, sin lock.</p>
 */
@Service
public class ContextoFinalizacionProyectoService {

    public static final String PERMISO_FINALIZAR_PROYECTO = "FINALIZAR_PROYECTO";

    private final PerfilRepository perfilRepository;
    private final ProyectoRepository proyectoRepository;
    private final PlanificacionRepository planificacionRepository;
    private final ActividadRepository actividadRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final CalificacionPerfilRepository calificacionPerfilRepository;
    private final ProyectoSecurityService proyectoSecurityService;
    private final Clock reloj;

    public ContextoFinalizacionProyectoService(
            PerfilRepository perfilRepository,
            ProyectoRepository proyectoRepository,
            PlanificacionRepository planificacionRepository,
            ActividadRepository actividadRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            CalificacionPerfilRepository calificacionPerfilRepository,
            ProyectoSecurityService proyectoSecurityService,
            Clock reloj) {
        this.perfilRepository = perfilRepository;
        this.proyectoRepository = proyectoRepository;
        this.planificacionRepository = planificacionRepository;
        this.actividadRepository = actividadRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.calificacionPerfilRepository = calificacionPerfilRepository;
        this.proyectoSecurityService = proyectoSecurityService;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public Contexto cargar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            boolean bloquear) {

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfil = perfilRepository
                .findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() != EstadoPerfil.Activo) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }

        Proyecto proyecto = (bloquear
                ? proyectoRepository.findByIdParaFinalizar(idProyecto)
                : proyectoRepository.findById(idProyecto))
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        proyectoSecurityService.validarPermiso(
                idProyecto, idPerfilActivo, PERMISO_FINALIZAR_PROYECTO);
        proyectoSecurityService.validarDirector(idProyecto, idPerfilActivo);

        LocalDateTime ahora = LocalDateTime.now(reloj).withNano(0);
        LocalDate hoy = ahora.toLocalDate();
        List<BloqueoFinalizacionResponse> bloqueos = new ArrayList<>();
        List<AdvertenciaFinalizacionResponse> advertencias = new ArrayList<>();
        long diasAnticipacion = 0;

        if (proyecto.getEstado() == EstadoProyecto.FINALIZADO) {
            bloqueos.add(new BloqueoFinalizacionResponse(
                    "PROYECTO_YA_FINALIZADO",
                    "El proyecto ya está finalizado.",
                    "PROYECTO",
                    idProyecto));
        } else if (proyecto.getEstado() != EstadoProyecto.CONFIRMADO) {
            bloqueos.add(new BloqueoFinalizacionResponse(
                    "ESTADO_PROYECTO_INVALIDO",
                    "Solo se puede finalizar un proyecto en estado Confirmado. Estado actual: "
                            + proyecto.getEstado().getNombre() + ".",
                    "PROYECTO",
                    idProyecto));
        }

        Planificacion planificacion = (bloquear
                ? planificacionRepository.findByProyectoIdProyectoForUpdate(idProyecto)
                : planificacionRepository.findByProyectoIdProyecto(idProyecto))
                .orElse(null);

        if (planificacion == null) {
            bloqueos.add(new BloqueoFinalizacionResponse(
                    "PLANIFICACION_NO_ENCONTRADA",
                    "El proyecto no tiene una planificación asociada.",
                    "PROYECTO",
                    idProyecto));
        } else if (planificacion.getFechaEntrega() == null) {
            bloqueos.add(new BloqueoFinalizacionResponse(
                    "FECHA_ENTREGA_NO_DEFINIDA",
                    "La planificación no tiene fecha de entrega.",
                    "PLANIFICACION",
                    planificacion.getIdPlanificacion()));
        } else if (planificacion.getFechaEntrega().isAfter(hoy)) {
            diasAnticipacion = ChronoUnit.DAYS.between(
                    hoy, planificacion.getFechaEntrega());
            advertencias.add(new AdvertenciaFinalizacionResponse(
                    "FECHA_ENTREGA_FUTURA",
                    "La fecha de entrega estipulada es "
                            + planificacion.getFechaEntrega()
                            + ". Hoy es " + hoy
                            + "; se finalizaría " + diasAnticipacion
                            + (diasAnticipacion == 1 ? " día antes." : " días antes."),
                    "PLANIFICACION",
                    planificacion.getIdPlanificacion()));
        }

        List<Actividad> actividades = planificacion == null
                ? List.of()
                : actividadRepository.findByPlanificacionProyectoIdProyecto(idProyecto);

        if (actividades.isEmpty()) {
            bloqueos.add(new BloqueoFinalizacionResponse(
                    "SIN_ACTIVIDADES",
                    "El proyecto no tiene actividades en la planificación.",
                    "PROYECTO",
                    idProyecto));
        }

        for (Actividad actividad : actividades) {
            if (actividad.getEstado() == EstadoActividad.PENDIENTE
                    || actividad.getEstado() == EstadoActividad.EN_CURSO) {
                bloqueos.add(new BloqueoFinalizacionResponse(
                        "ACTIVIDAD_NO_FINALIZADA",
                        "La actividad '" + actividad.getNombre()
                                + "' está en estado "
                                + actividad.getEstado().getNombre()
                                + " y debe estar Finalizada o Cancelada.",
                        "ACTIVIDAD",
                        actividad.getIdActividad()));
            }
        }

        List<MiembroProyecto> miembrosActivos = miembroProyectoRepository
                .buscarActivosDeProyectoConPerfilProfesionYRol(idProyecto);

        List<CalificacionPerfil> calificacionesExistentes =
                calificacionPerfilRepository
                        .findByProyectoIdProyectoAndPerfilEmisorIdPerfil(
                                idProyecto, idPerfilActivo);

        Set<Long> perfilesYaCalificados = new HashSet<>();
        for (CalificacionPerfil calificacion : calificacionesExistentes) {
            if (calificacion.getPerfilReceptor() != null
                    && calificacion.getPerfilReceptor().getIdPerfil() != null) {
                perfilesYaCalificados.add(
                        calificacion.getPerfilReceptor().getIdPerfil());
            }
        }

        List<MiembroCalificableResponse> miembrosACalificar = miembrosActivos.stream()
                .filter(miembro -> miembro.getPerfil() != null
                        && miembro.getPerfil().getIdPerfil() != null
                        && !idPerfilActivo.equals(miembro.getPerfil().getIdPerfil()))
                .map(miembro -> toMiembroCalificable(
                        miembro, perfilesYaCalificados.contains(
                                miembro.getPerfil().getIdPerfil())))
                .toList();

        return new Contexto(
                proyecto,
                perfil,
                planificacion,
                ahora,
                List.copyOf(advertencias),
                diasAnticipacion > 0,
                diasAnticipacion,
                actividades,
                miembrosActivos,
                calificacionesExistentes,
                miembrosACalificar,
                List.copyOf(bloqueos));
    }

    private MiembroCalificableResponse toMiembroCalificable(
            MiembroProyecto miembro,
            boolean yaCalificado) {

        Perfil perfil = miembro.getPerfil();
        RolProyectoUsuario rol = RolProyectoUsuario.desde(miembro.getRolProyecto());

        return new MiembroCalificableResponse(
                perfil.getIdPerfil(),
                perfil.getNombreArtistico(),
                perfil.getUsuario() != null ? perfil.getUsuario().getNombre() : null,
                perfil.getUsuario() != null ? perfil.getUsuario().getApellido() : null,
                rol != null ? rol.name() : null,
                yaCalificado);
    }

    public record Contexto(
            Proyecto proyecto,
            Perfil perfilActor,
            Planificacion planificacion,
            LocalDateTime ahora,
            List<AdvertenciaFinalizacionResponse> advertencias,
            boolean requiereConfirmacionPrematura,
            long diasAnticipacion,
            List<Actividad> actividades,
            List<MiembroProyecto> miembrosActivos,
            List<CalificacionPerfil> calificacionesExistentes,
            List<MiembroCalificableResponse> miembrosACalificar,
            List<BloqueoFinalizacionResponse> bloqueos) {
    }
}
