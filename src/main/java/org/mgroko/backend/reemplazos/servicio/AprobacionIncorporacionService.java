package org.mgroko.backend.reemplazos.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.servicio.GestionTerminosCondicionesService;
import org.mgroko.backend.proyectos.evento.DisponibilidadModificadaEvent;
import org.mgroko.backend.proyectos.exception.RolProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.servicio.CupoRequerimientoService;
import org.mgroko.backend.proyectos.servicio.MotivoReprogramacion;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.proyectos.servicio.ReprogramarProyectoService;
import org.mgroko.backend.reemplazos.dto.AprobarCandidatoRequest;
import org.mgroko.backend.reemplazos.dto.CandidatoReemplazoResponse;
import org.mgroko.backend.reemplazos.enums.EstadoCandidatoReemplazo;
import org.mgroko.backend.reemplazos.enums.EstadoReemplazo;
import org.mgroko.backend.reemplazos.enums.TipoDisponibilidad;
import org.mgroko.backend.reemplazos.enums.TipoReemplazo;
import org.mgroko.backend.reemplazos.exception.CandidatoReemplazoNoEncontradoException;
import org.mgroko.backend.reemplazos.exception.ReemplazoNoGestionableException;
import org.mgroko.backend.reemplazos.mapper.ReemplazoMapper;
import org.mgroko.backend.reemplazos.modelo.Reemplazo;
import org.mgroko.backend.reemplazos.modelo.ReemplazoCandidato;
import org.mgroko.backend.reemplazos.repositorio.ReemplazoCandidatoRepository;
import org.mgroko.backend.reemplazos.repositorio.ReemplazoRepository;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.RolProyectoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aprobación o rechazo de la incorporación de un candidato que aceptó.
 */
@Service
public class AprobacionIncorporacionService {

    private static final String ROL_MIEMBRO = "MIEMBRO";

    private final ReemplazoRepository reemplazoRepository;
    private final ReemplazoCandidatoRepository candidatoRepository;
    private final ProyectoSecurityService proyectoSecurityService;
    private final ReemplazoQueryService queryService;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final AsignacionActividadRepository asignacionActividadRepository;
    private final RolProyectoRepository rolProyectoRepository;
    private final CupoRequerimientoService cupoRequerimientoService;
    private final CandidatoDisponibilidadService disponibilidadService;
    private final ReprogramarProyectoService reprogramarProyectoService;
    private final ReemplazoNotificacionService notificacionService;
    private final ApplicationEventPublisher eventPublisher;
    private final GestionTerminosCondicionesService gestionTerminosCondicionesService;
    private final Clock reloj;

    public AprobacionIncorporacionService(
            ReemplazoRepository reemplazoRepository,
            ReemplazoCandidatoRepository candidatoRepository,
            ProyectoSecurityService proyectoSecurityService,
            ReemplazoQueryService queryService,
            MiembroProyectoRepository miembroProyectoRepository,
            AsignacionActividadRepository asignacionActividadRepository,
            RolProyectoRepository rolProyectoRepository,
            CupoRequerimientoService cupoRequerimientoService,
            CandidatoDisponibilidadService disponibilidadService,
            ReprogramarProyectoService reprogramarProyectoService,
            ReemplazoNotificacionService notificacionService,
            ApplicationEventPublisher eventPublisher,
            GestionTerminosCondicionesService gestionTerminosCondicionesService,
            Clock reloj) {
        this.reemplazoRepository = reemplazoRepository;
        this.candidatoRepository = candidatoRepository;
        this.proyectoSecurityService = proyectoSecurityService;
        this.queryService = queryService;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.asignacionActividadRepository = asignacionActividadRepository;
        this.rolProyectoRepository = rolProyectoRepository;
        this.cupoRequerimientoService = cupoRequerimientoService;
        this.disponibilidadService = disponibilidadService;
        this.reprogramarProyectoService = reprogramarProyectoService;
        this.notificacionService = notificacionService;
        this.eventPublisher = eventPublisher;
        this.gestionTerminosCondicionesService = gestionTerminosCondicionesService;
        this.reloj = reloj;
    }

    @Transactional
    public CandidatoReemplazoResponse aprobar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Long idReemplazo,
            Long idCandidato) {
        return aprobar(idUsuario, idPerfilActivo, idProyecto, idReemplazo,
                idCandidato, null);
    }

    @Transactional
    public CandidatoReemplazoResponse aprobar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Long idReemplazo,
            Long idCandidato,
            AprobarCandidatoRequest request) {

        queryService.validarPerfilActivo(idUsuario, idPerfilActivo);
        proyectoSecurityService.validarDirector(idProyecto, idPerfilActivo);

        Reemplazo reemplazo = reemplazoRepository.findByIdForUpdate(idReemplazo)
                .filter(r -> r.getProyecto().getIdProyecto().equals(idProyecto))
                .orElseThrow(() -> new CandidatoReemplazoNoEncontradoException(idCandidato));

        ReemplazoCandidato candidato = candidatoRepository.findByIdForUpdate(idCandidato)
                .filter(c -> c.getReemplazo().getIdReemplazo().equals(idReemplazo))
                .orElseThrow(() -> new CandidatoReemplazoNoEncontradoException(idCandidato));

        validarEstadoAprobacion(reemplazo, candidato);
        validarProyectoYVacante(reemplazo);
        validarCandidatoActivo(candidato);

        Long idTycAceptado = gestionTerminosCondicionesService.validarAceptacion(
                candidato.getPerfil().getIdPerfil(),
                request != null ? request.aceptacionTerminos() : null);

        Long idMiembro;
        Long idTycRegistrado = idTycAceptado;
        Long idAsignacion = null;

        if (reemplazo.getTipo() == TipoReemplazo.ACTIVIDAD) {
            var disponibilidad = disponibilidadService.evaluar(
                    reemplazo.getProyecto(),
                    reemplazo.getActividad(),
                    candidato.getUsuario().getIdUsuario());
            if (!disponibilidad.disponible()) {
                throw new ReemplazoNoGestionableException(
                        "El candidato ya no tiene disponibilidad para la actividad.",
                        "CANDIDATO_SIN_DISPONIBILIDAD");
            }

            if (disponibilidad.tipo() == TipoDisponibilidad.CON_REPROGRAMACION) {
                reprogramarProyectoService.reprogramarActividad(
                        idProyecto,
                        reemplazo.getActividad().getIdActividad(),
                        MotivoReprogramacion.ASIGNACION,
                        Map.of(
                                reemplazo.getActividad().getIdActividad(),
                                List.of(candidato.getUsuario().getIdUsuario())));
            }

            MiembroProyecto miembro = obtenerOCrearMiembro(
                    reemplazo, candidato, null, idTycAceptado);
            idMiembro = miembro.getIdMiembro();
            idTycRegistrado = miembro.getIdTycAceptado();

            if (asignacionActividadRepository
                    .existsByMiembro_IdMiembroAndActividad_IdActividad(
                            miembro.getIdMiembro(),
                            reemplazo.getActividad().getIdActividad())) {
                throw new ReemplazoNoGestionableException(
                        "El candidato ya está asignado a esta actividad.",
                        "CANDIDATO_YA_ASIGNADO");
            }

            AsignacionActividad asignacion = asignacionActividadRepository.saveAndFlush(
                    AsignacionActividad.builder()
                            .miembro(miembro)
                            .actividad(reemplazo.getActividad())
                            .requerimientoActividad(reemplazo.getRequerimientoActividad())
                            .build());
            idAsignacion = asignacion.getIdAsignacionAct();

            eventPublisher.publishEvent(new DisponibilidadModificadaEvent(
                    candidato.getUsuario().getIdUsuario(),
                    MotivoReprogramacion.ASIGNACION,
                    idProyecto));
        } else {
            MiembroProyecto miembro = obtenerOCrearMiembro(
                    reemplazo,
                    candidato,
                    reemplazo.getRequerimientoGral(),
                    idTycAceptado);
            idMiembro = miembro.getIdMiembro();
            idTycRegistrado = miembro.getIdTycAceptado();
        }

        LocalDateTime ahora = LocalDateTime.now(reloj);
        candidato.setEstado(EstadoCandidatoReemplazo.APROBADO);
        candidato.setFechaDecision(ahora);
        candidato.setIdUsuarioDecision(idUsuario);
        candidato.setIdMiembroCreado(idMiembro);
        candidato.setIdAsignacionCreada(idAsignacion);
        candidato.setIdTycAceptado(idTycRegistrado);
        candidatoRepository.saveAndFlush(candidato);

        reemplazo.setEstado(EstadoReemplazo.CUBIERTO);
        reemplazo.setFechaCierre(ahora);
        reemplazo.setIdMiembroResultante(idMiembro);
        reemplazo.setIdAsignacionResultante(idAsignacion);
        reemplazoRepository.saveAndFlush(reemplazo);

        // El primero que aceptó y fue aprobado cubre la vacante: se eliminan
        // las demás invitaciones del proceso, salvo los rechazos del
        // profesional, que se conservan como historial.
        candidatoRepository.deleteOtrosCandidatos(idReemplazo, idCandidato);

        notificacionService.notificarCandidato(
                candidato,
                NotificacionService.TIPO_REEMPLAZO_INCORPORACION_APROBADA,
                "Tu incorporación para cubrir " + descripcionVacante(reemplazo)
                        + " en el proyecto '" + reemplazo.getProyecto().getNombre()
                        + "' fue aprobada.");

        notificacionService.notificarDirectores(
                reemplazo,
                NotificacionService.TIPO_REEMPLAZO_INCORPORACION_APROBADA,
                "Se aprobó la incorporación de '"
                        + candidato.getPerfil().getNombreArtistico()
                        + "' para cubrir " + descripcionVacante(reemplazo)
                        + " en el proyecto '" + reemplazo.getProyecto().getNombre() + "'.");

        return ReemplazoMapper.toCandidato(candidato);
    }

    @Transactional
    public CandidatoReemplazoResponse rechazar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            Long idReemplazo,
            Long idCandidato,
            String motivo) {

        queryService.validarPerfilActivo(idUsuario, idPerfilActivo);
        proyectoSecurityService.validarDirector(idProyecto, idPerfilActivo);

        Reemplazo reemplazo = reemplazoRepository.findByIdForUpdate(idReemplazo)
                .filter(r -> r.getProyecto().getIdProyecto().equals(idProyecto))
                .orElseThrow(() -> new CandidatoReemplazoNoEncontradoException(idCandidato));

        ReemplazoCandidato candidato = candidatoRepository.findByIdForUpdate(idCandidato)
                .filter(c -> c.getReemplazo().getIdReemplazo().equals(idReemplazo))
                .orElseThrow(() -> new CandidatoReemplazoNoEncontradoException(idCandidato));

        validarEstadoAprobacion(reemplazo, candidato);

        candidato.setEstado(EstadoCandidatoReemplazo.RECHAZADO_ADMIN);
        candidato.setFechaDecision(LocalDateTime.now(reloj));
        candidato.setIdUsuarioDecision(idUsuario);
        candidato.setMotivoDecision(normalizarMotivo(motivo));
        candidatoRepository.saveAndFlush(candidato);

        long invitacionesRestantes = candidatoRepository
                .countByReemplazoIdReemplazoAndEstadoIn(
                        idReemplazo,
                        List.of(EstadoCandidatoReemplazo.INVITADO));

        reemplazo.setEstado(invitacionesRestantes > 0
                ? EstadoReemplazo.INVITACIONES_ENVIADAS
                : EstadoReemplazo.SIN_CANDIDATOS);
        reemplazoRepository.saveAndFlush(reemplazo);

        notificacionService.notificarCandidato(
                candidato,
                NotificacionService.TIPO_REEMPLAZO_INCORPORACION_RECHAZADA,
                "Tu incorporación para cubrir " + descripcionVacante(reemplazo)
                        + " en el proyecto '" + reemplazo.getProyecto().getNombre()
                        + "' fue rechazada.");

        return ReemplazoMapper.toCandidato(candidato);
    }

    private void validarEstadoAprobacion(
            Reemplazo reemplazo,
            ReemplazoCandidato candidato) {

        if (reemplazo.getEstado() != EstadoReemplazo.PENDIENTE_APROBACION
                || candidato.getEstado()
                        != EstadoCandidatoReemplazo.ACEPTADO_PENDIENTE_APROBACION) {
            throw new ReemplazoNoGestionableException(
                    "La incorporación no está pendiente de aprobación.",
                    "INCORPORACION_NO_PENDIENTE");
        }
    }

    private void validarProyectoYVacante(Reemplazo reemplazo) {
        if (reemplazo.getProyecto().getEstado() != EstadoProyecto.CONFIRMADO) {
            throw new ReemplazoNoGestionableException(
                    "El proyecto ya no está confirmado.",
                    "PROYECTO_NO_CONFIRMADO");
        }

        if (reemplazo.getTipo() == TipoReemplazo.ACTIVIDAD) {
            if (reemplazo.getActividad() == null
                    || reemplazo.getActividad().getEstado() == EstadoActividad.FINALIZADA
                    || reemplazo.getActividad().getEstado() == EstadoActividad.CANCELADA) {
                throw new ReemplazoNoGestionableException(
                        "La actividad ya no admite incorporaciones.",
                        "ACTIVIDAD_NO_GESTIONABLE");
            }
            if (reemplazo.getRequerimientoActividad() == null
                    || reemplazo.getRequerimientoActividad().getFechaArchivado() != null) {
                throw new ReemplazoNoGestionableException(
                        "El requerimiento de actividad ya no está activo.",
                        "REQUERIMIENTO_NO_ACTIVO");
            }
        } else if (reemplazo.getRequerimientoGral() == null
                || reemplazo.getRequerimientoGral().getFechaArchivado() != null) {
            throw new ReemplazoNoGestionableException(
                    "El requerimiento general ya no está activo.",
                    "REQUERIMIENTO_NO_ACTIVO");
        }

        boolean satisfecho = reemplazo.getTipo() == TipoReemplazo.ACTIVIDAD
                ? cupoRequerimientoService
                        .calcularActividad(reemplazo.getRequerimientoActividad())
                        .satisfecho()
                : cupoRequerimientoService
                        .calcularGeneral(reemplazo.getRequerimientoGral())
                        .satisfecho();
        if (satisfecho) {
            throw new ReemplazoNoGestionableException(
                    "La vacante ya está cubierta.",
                    "VACANTE_YA_CUBIERTA");
        }
    }

    private void validarCandidatoActivo(ReemplazoCandidato candidato) {
        if (candidato.getPerfil() == null || candidato.getUsuario() == null
                || candidato.getPerfil().getEstado() != EstadoPerfil.ACTIVO
                || candidato.getUsuario().getEstado() != EstadoUsuario.ACTIVO) {
            throw new ReemplazoNoGestionableException(
                    "El perfil del candidato ya no está activo.",
                    "CANDIDATO_INACTIVO");
        }
    }

    private MiembroProyecto obtenerOCrearMiembro(
            Reemplazo reemplazo,
            ReemplazoCandidato candidato,
            RequerimientoGralProyecto requerimientoGral,
            Long idTycAceptado) {

        var existente = miembroProyectoRepository.findMiembroActivoConRol(
                reemplazo.getProyecto().getIdProyecto(),
                candidato.getPerfil().getIdPerfil());
        if (existente.isPresent()) {
            MiembroProyecto miembro = existente.get();
            boolean requiereGuardar = false;
            if (requerimientoGral != null && miembro.getRequerimientoGral() == null) {
                miembro.setRequerimientoGral(requerimientoGral);
                requiereGuardar = true;
            }
            if (miembro.getIdTycAceptado() == null && idTycAceptado != null) {
                miembro.setIdTycAceptado(idTycAceptado);
                requiereGuardar = true;
            }
            if (requiereGuardar) {
                miembroProyectoRepository.saveAndFlush(miembro);
            }
            return miembro;
        }

        RolProyecto rolMiembro = rolProyectoRepository.findByCodigo(ROL_MIEMBRO)
                .orElseThrow(() -> new RolProyectoNoEncontradoException(ROL_MIEMBRO));

        try {
            return miembroProyectoRepository.saveAndFlush(MiembroProyecto.builder()
                    .proyecto(reemplazo.getProyecto())
                    .perfil(candidato.getPerfil())
                    .rolProyecto(rolMiembro)
                    .estadoParticipacion(EstadoParticipacion.ACTIVO)
                    .fechaAlta(LocalDateTime.now(reloj))
                    .requerimientoGral(requerimientoGral)
                    .idTycAceptado(idTycAceptado)
                    .build());
        } catch (DataIntegrityViolationException ex) {
            throw new ReemplazoNoGestionableException(
                    "El perfil ya es miembro activo del proyecto.",
                    "CANDIDATO_YA_ES_MIEMBRO");
        }
    }

    private String descripcionVacante(Reemplazo reemplazo) {
        if (reemplazo.getTipo() == TipoReemplazo.GENERAL) {
            return "la profesión '"
                    + reemplazo.getRequerimientoGral().getProfesion().getNombre()
                    + "'";
        }
        return "la actividad '" + reemplazo.getActividad().getNombre() + "'";
    }

    private String normalizarMotivo(String motivo) {
        if (motivo == null) {
            return null;
        }
        String normalizado = motivo.trim();
        return normalizado.isEmpty()
                ? null
                : normalizado.length() > 200
                        ? normalizado.substring(0, 200)
                        : normalizado;
    }
}
