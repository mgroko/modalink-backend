package org.mgroko.backend.proyectos.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.RolProyectoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionDestinoResolver;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.proyectos.dto.EliminarIntegranteRequest;
import org.mgroko.backend.proyectos.exception.AutoeliminacionIntegranteException;
import org.mgroko.backend.proyectos.exception.EstadoParticipacionInvalidoException;
import org.mgroko.backend.proyectos.exception.EstadoProyectoNoPermiteEliminarIntegrantesException;
import org.mgroko.backend.proyectos.exception.MiembroProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.exception.MotivoEliminacionIntegranteObligatorioException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.exception.UltimoDirectorActivoException;
import org.mgroko.backend.reemplazos.enums.OrigenReemplazo;
import org.mgroko.backend.reemplazos.evento.ReemplazoNecesarioEvent;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.mgroko.backend.auditoria.servicio.AuditarOperacion;

/**
 * UC-38: el Director elimina lógicamente a un integrante del proyecto.
 *
 * <p>La membresía no se borra: pasa a {@code ELIMINADO} y conserva el motivo y
 * la fecha de baja. Las asignaciones a actividades tampoco se borran. Las
 * consultas operativas filtran miembros activos, por lo que la vacante se
 * reabre automáticamente y las asignaciones quedan como historial.</p>
 */
@Service
public class EliminarIntegranteService {

    public static final String PERMISO_ELIMINAR_INTEGRANTE =
            ProyectoSecurityService.PERMISO_ELIMINAR_INTEGRANTE;

    private static final int MOTIVO_MAX_LENGTH = 100;

    private final PerfilRepository perfilRepository;
    private final PlanificacionRepository planificacionRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final ProyectoSecurityService proyectoSecurityService;
    private final NotificacionService notificacionService;
    private final RequerimientoSnapshotService requerimientoSnapshotService;
    private final Clock reloj;

    private ApplicationEventPublisher eventPublisher;

    public EliminarIntegranteService(
            PerfilRepository perfilRepository,
            PlanificacionRepository planificacionRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            ProyectoSecurityService proyectoSecurityService,
            NotificacionService notificacionService,
            RequerimientoSnapshotService requerimientoSnapshotService,
            Clock reloj) {
        this.perfilRepository = perfilRepository;
        this.planificacionRepository = planificacionRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.proyectoSecurityService = proyectoSecurityService;
        this.notificacionService = notificacionService;
        this.requerimientoSnapshotService = requerimientoSnapshotService;
        this.reloj = reloj;
    }

    /**
     * Publicador opcional para no cambiar el constructor usado por los tests
     * unitarios existentes. En producción Spring inyecta el publisher real.
     */
    @Autowired(required = false)
    public void setEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    @AuditarOperacion
    public void eliminar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Long idMiembro,
            EliminarIntegranteRequest request) {

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfilActor = perfilRepository
                .findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfilActor.getEstado() != EstadoPerfil.Activo) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }

        // Se bloquea la planificación para serializar contra asignaciones y
        // eliminaciones de actividades concurrentes.
        Planificacion planificacion = planificacionRepository
                .findByProyectoIdProyectoForUpdate(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        Proyecto proyecto = planificacion.getProyecto();

        proyectoSecurityService.validarPermiso(
                idProyecto,
                idPerfilActivo,
                PERMISO_ELIMINAR_INTEGRANTE);
        proyectoSecurityService.validarDirector(idProyecto, idPerfilActivo);

        if (proyecto.getEstado() != EstadoProyecto.PUBLICADO
                && proyecto.getEstado() != EstadoProyecto.CONFIRMADO) {
            throw new EstadoProyectoNoPermiteEliminarIntegrantesException(
                    proyecto.getEstado().getNombre());
        }

        MiembroProyecto miembro = miembroProyectoRepository
                .findByIdMiembroAndProyectoForUpdate(idMiembro, idProyecto)
                .orElseThrow(() -> new MiembroProyectoNoEncontradoException(idMiembro));

        if (miembro.getEstadoParticipacion() != EstadoParticipacion.ACTIVO) {
            throw new EstadoParticipacionInvalidoException(
                    miembro.getEstadoParticipacion() != null
                            ? miembro.getEstadoParticipacion().name()
                            : "DESCONOCIDO");
        }

        String motivo = normalizarMotivo(request);

        if (miembro.getPerfil() != null
                && idPerfilActivo.equals(miembro.getPerfil().getIdPerfil())) {
            throw new AutoeliminacionIntegranteException();
        }

        boolean esDirector = RolProyectoUsuario.desde(miembro.getRolProyecto())
                == RolProyectoUsuario.DIRECTOR;

        if (esDirector
                && miembroProyectoRepository.countOtrosDirectoresActivos(
                        idProyecto, idMiembro) < 1) {
            throw new UltimoDirectorActivoException();
        }

        LocalDateTime fechaBaja = LocalDateTime.now(reloj);
        miembro.setEstadoParticipacion(EstadoParticipacion.ELIMINADO);
        miembro.setMotivoBaja(motivo);
        miembro.setFechaBaja(fechaBaja);
        miembro.setPerfilAutorBaja(perfilActor);

        // El miembro ya no está activo: el snapshot puede repuntar su
        // historial sin afectar asignaciones de miembros activos.
        requerimientoSnapshotService.prepararHistorialBaja(
                miembro, idProyecto, fechaBaja);

        miembroProyectoRepository.saveAndFlush(miembro);

        notificarIntegranteEliminado(proyecto, miembro.getPerfil(), motivo);
        publicarReemplazo(proyecto, miembro);
    }

    private void publicarReemplazo(Proyecto proyecto, MiembroProyecto miembro) {
        if (eventPublisher == null
                || proyecto == null
                || proyecto.getEstado() != EstadoProyecto.CONFIRMADO
                || miembro == null
                || miembro.getIdMiembro() == null
                || miembro.getPerfil() == null
                || miembro.getPerfil().getIdPerfil() == null) {
            return;
        }

        eventPublisher.publishEvent(new ReemplazoNecesarioEvent(
                proyecto.getIdProyecto(),
                miembro.getIdMiembro(),
                miembro.getPerfil().getIdPerfil(),
                OrigenReemplazo.ELIMINACION));
    }

    private String normalizarMotivo(EliminarIntegranteRequest request) {
        if (request == null
                || request.motivo() == null
                || request.motivo().isBlank()) {
            throw new MotivoEliminacionIntegranteObligatorioException();
        }

        String normalizado = request.motivo().trim();
        if (normalizado.length() > MOTIVO_MAX_LENGTH) {
            throw new MotivoEliminacionIntegranteObligatorioException(
                    "El motivo de eliminación no puede superar los "
                            + MOTIVO_MAX_LENGTH + " caracteres.");
        }

        return normalizado;
    }

    private void notificarIntegranteEliminado(
            Proyecto proyecto,
            Perfil perfil,
            String motivo) {

        if (perfil == null || perfil.getUsuario() == null) {
            return;
        }

        Notificacion notificacion = notificacionService.crear(
                perfil.getUsuario(),
                perfil,
                NotificacionService.TIPO_INTEGRANTE_ELIMINADO,
                "Fuiste eliminado del proyecto '" + proyecto.getNombre()
                        + "'. Motivo: " + motivo,
                NotificacionDestinoResolver.ENTIDAD_PROYECTO,
                proyecto.getIdProyecto(),
                proyecto.getIdProyecto());

        notificacionService.registrarTodas(List.of(notificacion));
    }
}
