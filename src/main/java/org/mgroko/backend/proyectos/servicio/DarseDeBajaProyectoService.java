package org.mgroko.backend.proyectos.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
import org.mgroko.backend.proyectos.dto.DarseDeBajaProyectoRequest;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.proyectos.exception.EstadoParticipacionInvalidoException;
import org.mgroko.backend.proyectos.exception.EstadoProyectoNoPermiteBajaVoluntariaException;
import org.mgroko.backend.proyectos.exception.MotivoBajaVoluntariaObligatorioException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.exception.UltimoDirectorActivoException;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-40: baja voluntaria de un integrante del proyecto.
 *
 * <p>La operación es lógica, igual que UC-38: la membresía pasa a
 * {@code BAJA_VOLUNTARIA} y conserva el motivo y la fecha de baja. Las
 * asignaciones a actividades no se borran; quedan como historial. La vacante
 * se reabre automáticamente porque los cupos operativos cuentan solo miembros
 * activos.</p>
 */
@Service
public class DarseDeBajaProyectoService {

    private static final Logger LOG =
            LoggerFactory.getLogger(DarseDeBajaProyectoService.class);

    private static final int MOTIVO_MAX_LENGTH = 100;

    private final PerfilRepository perfilRepository;
    private final PlanificacionRepository planificacionRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final NotificacionService notificacionService;
    private final RequerimientoSnapshotService requerimientoSnapshotService;
    private final Clock reloj;

    public DarseDeBajaProyectoService(
            PerfilRepository perfilRepository,
            PlanificacionRepository planificacionRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            NotificacionService notificacionService,
            RequerimientoSnapshotService requerimientoSnapshotService,
            Clock reloj) {
        this.perfilRepository = perfilRepository;
        this.planificacionRepository = planificacionRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.notificacionService = notificacionService;
        this.requerimientoSnapshotService = requerimientoSnapshotService;
        this.reloj = reloj;
    }

    @Transactional
    public void darseDeBaja(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            DarseDeBajaProyectoRequest request) {

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfil = perfilRepository
                .findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() != EstadoPerfil.Activo) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }

        // Mismo orden de bloqueo que UC-38: primero la planificación y luego
        // la membresía, para serializar bajas y cambios de actividades.
        Planificacion planificacion = planificacionRepository
                .findByProyectoIdProyectoForUpdate(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        Proyecto proyecto = planificacion.getProyecto();
        validarEstadoProyecto(proyecto);

        MiembroProyecto miembro = buscarMembresiaActiva(idProyecto, idPerfilActivo);
        validarEstadoMembresia(miembro);

        String motivo = normalizarMotivo(request);
        validarUltimoDirector(proyecto, miembro);

        LocalDateTime fechaBaja = LocalDateTime.now(reloj);
        miembro.setEstadoParticipacion(EstadoParticipacion.BAJA_VOLUNTARIA);
        miembro.setMotivoBaja(motivo);
        miembro.setFechaBaja(fechaBaja);
        miembro.setPerfilAutorBaja(perfil);

        // El miembro ya no está activo: el snapshot puede repuntar su
        // historial sin afectar asignaciones de miembros activos.
        requerimientoSnapshotService.prepararHistorialBaja(
                miembro, idProyecto, fechaBaja);

        miembroProyectoRepository.saveAndFlush(miembro);

        notificarDirectores(proyecto, miembro.getPerfil(), motivo);
    }

    private MiembroProyecto buscarMembresiaActiva(Long idProyecto, Long idPerfil) {
        Optional<MiembroProyecto> membresiaActiva = miembroProyectoRepository
                .findMiembroActivoDeProyectoParaActualizar(idProyecto, idPerfil);

        if (membresiaActiva.isPresent()) {
            return membresiaActiva.get();
        }

        Optional<MiembroProyecto> historica = miembroProyectoRepository
                .findFirstByProyectoIdProyectoAndPerfilIdPerfilOrderByFechaAltaDesc(
                        idProyecto, idPerfil);

        if (historica.isPresent()) {
            EstadoParticipacion estado = historica.get().getEstadoParticipacion();
            throw new EstadoParticipacionInvalidoException(
                    estado != null ? estado.name() : "DESCONOCIDO");
        }

        throw new AccesoDenegadoProyectoException(
                "No sos miembro activo de este proyecto.");
    }

    private void validarEstadoProyecto(Proyecto proyecto) {
        if (proyecto.getEstado() != EstadoProyecto.PUBLICADO
                && proyecto.getEstado() != EstadoProyecto.CONFIRMADO) {
            throw new EstadoProyectoNoPermiteBajaVoluntariaException(
                    proyecto.getEstado() != null
                            ? proyecto.getEstado().getNombre()
                            : "DESCONOCIDO");
        }
    }

    private void validarEstadoMembresia(MiembroProyecto miembro) {
        if (miembro.getEstadoParticipacion() != EstadoParticipacion.ACTIVO) {
            throw new EstadoParticipacionInvalidoException(
                    miembro.getEstadoParticipacion() != null
                            ? miembro.getEstadoParticipacion().name()
                            : "DESCONOCIDO");
        }
    }

    private void validarUltimoDirector(Proyecto proyecto, MiembroProyecto miembro) {
        boolean esDirector = RolProyectoUsuario.desde(miembro.getRolProyecto())
                == RolProyectoUsuario.DIRECTOR;

        if (esDirector
                && miembroProyectoRepository.countOtrosDirectoresActivos(
                        proyecto.getIdProyecto(), miembro.getIdMiembro()) < 1) {
            throw new UltimoDirectorActivoException();
        }
    }

    private String normalizarMotivo(DarseDeBajaProyectoRequest request) {
        if (request == null
                || request.motivo() == null
                || request.motivo().isBlank()) {
            throw new MotivoBajaVoluntariaObligatorioException();
        }

        String normalizado = request.motivo().trim();
        if (normalizado.length() > MOTIVO_MAX_LENGTH) {
            throw new MotivoBajaVoluntariaObligatorioException(
                    "El motivo de la baja no puede superar los "
                            + MOTIVO_MAX_LENGTH + " caracteres.");
        }

        return normalizado;
    }

    private void notificarDirectores(
            Proyecto proyecto,
            Perfil perfilBaja,
            String motivo) {

        List<MiembroProyecto> directores = miembroProyectoRepository
                .buscarDirectoresDeProyectos(List.of(proyecto.getIdProyecto()));

        if (directores.isEmpty()) {
            LOG.warn("No hay directores activos para notificar la baja voluntaria "
                    + "del proyecto {}", proyecto.getIdProyecto());
            return;
        }

        String nombre = perfilBaja != null
                && perfilBaja.getNombreArtistico() != null
                && !perfilBaja.getNombreArtistico().isBlank()
                        ? perfilBaja.getNombreArtistico()
                        : "Un integrante";

        String mensaje = "El integrante '" + nombre + "' se dio de baja del proyecto '"
                + proyecto.getNombre() + "'. Motivo: " + motivo;

        List<Notificacion> notificaciones = new ArrayList<>();
        Set<Long> destinatarios = new HashSet<>();

        for (MiembroProyecto director : directores) {
            Perfil perfilDirector = director.getPerfil();
            if (perfilDirector == null || perfilDirector.getUsuario() == null) {
                continue;
            }
            if (perfilBaja != null
                    && perfilDirector.getIdPerfil().equals(perfilBaja.getIdPerfil())) {
                continue;
            }
            if (!destinatarios.add(perfilDirector.getIdPerfil())) {
                continue;
            }

            notificaciones.add(notificacionService.crear(
                    perfilDirector.getUsuario(),
                    perfilDirector,
                    NotificacionService.TIPO_INTEGRANTE_BAJA_VOLUNTARIA,
                    mensaje,
                    NotificacionDestinoResolver.ENTIDAD_PROYECTO,
                    proyecto.getIdProyecto(),
                    proyecto.getIdProyecto()));
        }

        notificacionService.registrarTodas(notificaciones);
    }
}
