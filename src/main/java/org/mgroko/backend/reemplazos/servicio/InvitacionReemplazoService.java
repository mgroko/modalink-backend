package org.mgroko.backend.reemplazos.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.proyectos.servicio.CupoRequerimientoService;
import org.mgroko.backend.reemplazos.dto.CandidatoReemplazoResponse;
import org.mgroko.backend.reemplazos.enums.EstadoCandidatoReemplazo;
import org.mgroko.backend.reemplazos.enums.EstadoReemplazo;
import org.mgroko.backend.reemplazos.enums.TipoReemplazo;
import org.mgroko.backend.reemplazos.exception.CandidatoReemplazoNoEncontradoException;
import org.mgroko.backend.reemplazos.exception.ReemplazoNoGestionableException;
import org.mgroko.backend.reemplazos.mapper.ReemplazoMapper;
import org.mgroko.backend.reemplazos.modelo.Reemplazo;
import org.mgroko.backend.reemplazos.modelo.ReemplazoCandidato;
import org.mgroko.backend.reemplazos.repositorio.ReemplazoCandidatoRepository;
import org.mgroko.backend.reemplazos.repositorio.ReemplazoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Respuesta del profesional a una invitación de reemplazo.
 */
@Service
public class InvitacionReemplazoService {

    private static final List<EstadoCandidatoReemplazo> ESTADOS_INVITACION =
            List.of(EstadoCandidatoReemplazo.INVITADO);

    private final ReemplazoRepository reemplazoRepository;
    private final ReemplazoCandidatoRepository candidatoRepository;
    private final ReemplazoQueryService queryService;
    private final CupoRequerimientoService cupoRequerimientoService;
    private final CandidatoDisponibilidadService disponibilidadService;
    private final ReemplazoNotificacionService notificacionService;
    private final Clock reloj;

    public InvitacionReemplazoService(
            ReemplazoRepository reemplazoRepository,
            ReemplazoCandidatoRepository candidatoRepository,
            ReemplazoQueryService queryService,
            CupoRequerimientoService cupoRequerimientoService,
            CandidatoDisponibilidadService disponibilidadService,
            ReemplazoNotificacionService notificacionService,
            Clock reloj) {
        this.reemplazoRepository = reemplazoRepository;
        this.candidatoRepository = candidatoRepository;
        this.queryService = queryService;
        this.cupoRequerimientoService = cupoRequerimientoService;
        this.disponibilidadService = disponibilidadService;
        this.notificacionService = notificacionService;
        this.reloj = reloj;
    }

    @Transactional
    public CandidatoReemplazoResponse aceptar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idCandidato) {

        ReemplazoCandidato referencia = candidatoRepository.findByIdConDetalle(idCandidato)
                .orElseThrow(() -> new CandidatoReemplazoNoEncontradoException(idCandidato));

        queryService.validarPerfilActivo(idUsuario, idPerfilActivo);
        validarDestinatario(referencia, idPerfilActivo);

        Reemplazo reemplazo = reemplazoRepository
                .findByIdForUpdate(referencia.getReemplazo().getIdReemplazo())
                .orElseThrow(() -> new CandidatoReemplazoNoEncontradoException(idCandidato));
        ReemplazoCandidato candidato = candidatoRepository.findByIdForUpdate(idCandidato)
                .orElseThrow(() -> new CandidatoReemplazoNoEncontradoException(idCandidato));

        validarDestinatario(candidato, idPerfilActivo);
        validarEstadoInvitacion(candidato);
        validarProcesoAbierto(reemplazo);
        validarProyectoYVacante(reemplazo);
        validarCandidatoActivo(candidato);
        validarDisponibilidad(reemplazo, candidato);

        LocalDateTime ahora = LocalDateTime.now(reloj);
        candidato.setEstado(EstadoCandidatoReemplazo.ACEPTADO_PENDIENTE_APROBACION);
        candidato.setFechaRespuesta(ahora);
        candidatoRepository.saveAndFlush(candidato);

        reemplazo.setEstado(EstadoReemplazo.PENDIENTE_APROBACION);
        reemplazoRepository.saveAndFlush(reemplazo);

        notificacionService.notificarDirectores(
                reemplazo,
                NotificacionService.TIPO_REEMPLAZO_ACEPTADO_PENDIENTE_APROBACION,
                "El perfil '" + candidato.getPerfil().getNombreArtistico()
                        + "' aceptó la invitación para cubrir "
                        + descripcionVacante(reemplazo)
                        + " en el proyecto '" + reemplazo.getProyecto().getNombre()
                        + "'. La incorporación espera tu aprobación.");

        return ReemplazoMapper.toCandidato(candidato);
    }

    @Transactional
    public CandidatoReemplazoResponse rechazar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idCandidato,
            String motivo) {

        ReemplazoCandidato referencia = candidatoRepository.findByIdConDetalle(idCandidato)
                .orElseThrow(() -> new CandidatoReemplazoNoEncontradoException(idCandidato));

        queryService.validarPerfilActivo(idUsuario, idPerfilActivo);
        validarDestinatario(referencia, idPerfilActivo);

        Reemplazo reemplazo = reemplazoRepository
                .findByIdForUpdate(referencia.getReemplazo().getIdReemplazo())
                .orElseThrow(() -> new CandidatoReemplazoNoEncontradoException(idCandidato));
        ReemplazoCandidato candidato = candidatoRepository.findByIdForUpdate(idCandidato)
                .orElseThrow(() -> new CandidatoReemplazoNoEncontradoException(idCandidato));

        validarDestinatario(candidato, idPerfilActivo);
        validarEstadoInvitacion(candidato);

        candidato.setEstado(EstadoCandidatoReemplazo.RECHAZADO);
        candidato.setFechaRespuesta(LocalDateTime.now(reloj));
        candidato.setMotivoRespuesta(normalizarMotivo(motivo));
        candidatoRepository.saveAndFlush(candidato);

        if (reemplazo.getEstado() == EstadoReemplazo.INVITACIONES_ENVIADAS
                && candidatoRepository.countByReemplazoIdReemplazoAndEstadoIn(
                        reemplazo.getIdReemplazo(),
                        ESTADOS_INVITACION) == 0) {
            reemplazo.setEstado(EstadoReemplazo.SIN_CANDIDATOS);
            reemplazoRepository.saveAndFlush(reemplazo);
        }

        notificacionService.notificarDirectores(
                reemplazo,
                NotificacionService.TIPO_REEMPLAZO_CANDIDATO_RECHAZO,
                "El perfil '" + candidato.getPerfil().getNombreArtistico()
                        + "' rechazó la invitación para cubrir "
                        + descripcionVacante(reemplazo)
                        + " en el proyecto '" + reemplazo.getProyecto().getNombre() + "'.");

        return ReemplazoMapper.toCandidato(candidato);
    }

    private void validarDestinatario(
            ReemplazoCandidato candidato,
            Long idPerfilActivo) {

        if (candidato.getPerfil() == null
                || candidato.getPerfil().getIdPerfil() == null
                || !candidato.getPerfil().getIdPerfil().equals(idPerfilActivo)) {
            throw new ReemplazoNoGestionableException(
                    "La invitación no pertenece al perfil activo.",
                    "INVITACION_NO_PERTENECE_AL_PERFIL");
        }
    }

    private void validarEstadoInvitacion(ReemplazoCandidato candidato) {
        if (candidato.getEstado() != EstadoCandidatoReemplazo.INVITADO) {
            throw new ReemplazoNoGestionableException(
                    "La invitación no está pendiente de respuesta.",
                    "INVITACION_NO_PENDIENTE");
        }
    }

    private void validarProcesoAbierto(Reemplazo reemplazo) {
        if (reemplazo.getEstado() == EstadoReemplazo.PENDIENTE_APROBACION) {
            throw new ReemplazoNoGestionableException(
                    "Ya hay un candidato esperando la aprobación del director.",
                    "REEMPLAZO_CON_CANDIDATO_PENDIENTE");
        }
        if (reemplazo.getEstado() != EstadoReemplazo.INVITACIONES_ENVIADAS) {
            throw new ReemplazoNoGestionableException(
                    "El proceso de reemplazo ya no admite respuestas.",
                    "REEMPLAZO_NO_ADMITE_RESPUESTAS");
        }
    }

    private void validarProyectoYVacante(Reemplazo reemplazo) {
        if (reemplazo.getProyecto().getEstado() != EstadoProyecto.CONFIRMADO) {
            throw new ReemplazoNoGestionableException(
                    "El proyecto ya no está confirmado.",
                    "PROYECTO_NO_CONFIRMADO");
        }

        if (reemplazo.getTipo() == TipoReemplazo.ACTIVIDAD) {
            var actividad = reemplazo.getActividad();
            if (actividad == null
                    || actividad.getEstado() == EstadoActividad.FINALIZADA
                    || actividad.getEstado() == EstadoActividad.CANCELADA) {
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

    private void validarDisponibilidad(
            Reemplazo reemplazo,
            ReemplazoCandidato candidato) {

        if (reemplazo.getTipo() != TipoReemplazo.ACTIVIDAD) {
            return;
        }

        var disponibilidad = disponibilidadService.evaluar(
                reemplazo.getProyecto(),
                reemplazo.getActividad(),
                candidato.getUsuario().getIdUsuario());

        if (!disponibilidad.disponible()) {
            throw new ReemplazoNoGestionableException(
                    "El candidato ya no tiene disponibilidad para la actividad.",
                    "CANDIDATO_SIN_DISPONIBILIDAD");
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
