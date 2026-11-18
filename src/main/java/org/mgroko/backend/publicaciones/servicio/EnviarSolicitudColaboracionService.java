package org.mgroko.backend.publicaciones.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.SolicitudColaboracion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.perfiles.servicio.PerfilActivoService;
import org.mgroko.backend.publicaciones.dto.EnviarSolicitudColaboracionRequest;
import org.mgroko.backend.publicaciones.dto.SolicitudColaboracionResponse;
import org.mgroko.backend.publicaciones.exception.PublicacionNoEncontradaException;
import org.mgroko.backend.publicaciones.exception.SolicitudColaboracionDuplicadaException;
import org.mgroko.backend.publicaciones.exception.SolicitudColaboracionInvalidaException;
import org.mgroko.backend.publicaciones.mapper.SolicitudColaboracionMapper;
import org.mgroko.backend.repositorio.ConexionRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.mgroko.backend.repositorio.SolicitudColaboracionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-61 - Enviar solicitud de colaboración.
 *
 * <p>La solicitud se crea en estado {@code PENDIENTE}. No agrega al perfil a
 * los colaboradores de la publicación: esa incorporación corresponde a
 * UC-62.</p>
 */
@Service
public class EnviarSolicitudColaboracionService {

    private final PerfilActivoService perfilActivoService;
    private final PerfilRepository perfilRepository;
    private final PublicacionRepository publicacionRepository;
    private final SolicitudColaboracionRepository solicitudColaboracionRepository;
    private final ConexionRepository conexionRepository;
    private final NotificacionService notificacionService;
    private final Clock reloj;

    public EnviarSolicitudColaboracionService(
            PerfilActivoService perfilActivoService,
            PerfilRepository perfilRepository,
            PublicacionRepository publicacionRepository,
            SolicitudColaboracionRepository solicitudColaboracionRepository,
            ConexionRepository conexionRepository,
            NotificacionService notificacionService,
            Clock reloj) {
        this.perfilActivoService = perfilActivoService;
        this.perfilRepository = perfilRepository;
        this.publicacionRepository = publicacionRepository;
        this.solicitudColaboracionRepository = solicitudColaboracionRepository;
        this.conexionRepository = conexionRepository;
        this.notificacionService = notificacionService;
        this.reloj = reloj;
    }

    @Transactional
    public SolicitudColaboracionResponse enviar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idPublicacion,
            EnviarSolicitudColaboracionRequest request) {

        Perfil remitente = perfilActivoService.resolver(idUsuario, idPerfilActivo);

        Publicacion publicacion = publicacionRepository
                .findByIdPublicacionAndPerfilIdPerfil(idPublicacion, remitente.getIdPerfil())
                .orElseThrow(() -> new PublicacionNoEncontradaException(idPublicacion));

        if (publicacion.getEstado() != EstadoPublicacion.ACTIVA) {
            throw new SolicitudColaboracionInvalidaException(
                    "Solo se puede invitar a colaborar en publicaciones activas.",
                    "PUBLICACION_NO_DISPONIBLE");
        }

        Perfil destinatario = resolverDestinatario(request.idPerfilDestinatario());

        validarPerfilesDistintos(remitente, destinatario);

        boolean mismoUsuario = remitente.getUsuario() != null
                && destinatario.getUsuario() != null
                && remitente.getUsuario().getIdUsuario()
                        .equals(destinatario.getUsuario().getIdUsuario());

        if (!mismoUsuario && !conexionRepository.existsActivaEntre(
                remitente.getIdPerfil(), destinatario.getIdPerfil())) {
            throw new SolicitudColaboracionInvalidaException(
                    "El perfil destinatario no pertenece a tu red de contactos.",
                    "DESTINATARIO_FUERA_DE_RED");
        }

        if (publicacionRepository.existsColaborador(
                idPublicacion, destinatario.getIdPerfil())) {
            throw new SolicitudColaboracionInvalidaException(
                    "El perfil destinatario ya es colaborador de la publicación.",
                    "DESTINATARIO_YA_ES_COLABORADOR");
        }

        if (solicitudColaboracionRepository
                .existsByPublicacionIdPublicacionAndPerfilDestinatarioIdPerfilAndEstado(
                        idPublicacion,
                        destinatario.getIdPerfil(),
                        EstadoSolicitud.PENDIENTE)) {
            throw new SolicitudColaboracionDuplicadaException(
                    "Ya existe una solicitud de colaboración pendiente para ese perfil.");
        }

        SolicitudColaboracion solicitud = SolicitudColaboracion.builder()
                .publicacion(publicacion)
                .perfilRemitente(remitente)
                .perfilDestinatario(destinatario)
                .estado(EstadoSolicitud.PENDIENTE)
                .fechaEnvio(LocalDateTime.now(reloj))
                .build();

        try {
            solicitud = solicitudColaboracionRepository.saveAndFlush(solicitud);
        } catch (DataIntegrityViolationException ex) {
            if (mensajeContiene(ex, "UQ_solicitud_colaboracion_pendiente")) {
                throw new SolicitudColaboracionDuplicadaException(
                        "Ya existe una solicitud de colaboración pendiente para ese perfil.");
            }
            if (mensajeContiene(ex, "chk_solicitud_colaboracion_no_auto")) {
                throw new SolicitudColaboracionInvalidaException(
                        "No se puede enviar una solicitud al mismo perfil.",
                        "AUTOINVITACION");
            }
            throw new SolicitudColaboracionInvalidaException(
                    "No se pudo registrar la solicitud de colaboración.");
        }

        notificarSolicitud(destinatario, remitente, publicacion);
        return SolicitudColaboracionMapper.toResponse(solicitud);
    }

    private Perfil resolverDestinatario(Long idPerfilDestinatario) {
        Perfil destinatario = perfilRepository.findById(idPerfilDestinatario)
                .orElseThrow(PerfilNoEncontradoException::new);

        // Mismas reglas de visibilidad que UC-14/UC-43 para perfiles de
        // terceros.
        if (destinatario.getEstado() != EstadoPerfil.ACTIVO
                || destinatario.getUsuario() == null
                || destinatario.getUsuario().getEstado() != EstadoUsuario.ACTIVO) {
            throw new PerfilNoEncontradoException();
        }

        return destinatario;
    }

    private void validarPerfilesDistintos(Perfil remitente, Perfil destinatario) {
        if (remitente.getIdPerfil().equals(destinatario.getIdPerfil())) {
            throw new SolicitudColaboracionInvalidaException(
                    "No se puede enviar una solicitud al mismo perfil.",
                    "AUTOINVITACION");
        }
    }

    private void notificarSolicitud(
            Perfil destinatario,
            Perfil remitente,
            Publicacion publicacion) {

        String mensaje = "El perfil '" + remitente.getNombreArtistico()
                + "' te invitó a colaborar en la publicación '"
                + publicacion.getTitulo() + "'.";

        notificacionService.registrarTodas(List.of(
                notificacionService.crear(
                        destinatario.getUsuario(),
                        destinatario,
                        NotificacionService.TIPO_SOLICITUD_COLABORACION_RECIBIDA,
                        mensaje,
                        NotificacionService.ENTIDAD_PUBLICACION,
                        publicacion.getIdPublicacion())));
    }

    private boolean mensajeContiene(DataIntegrityViolationException ex, String texto) {
        Throwable causa = ex.getMostSpecificCause();
        return causa != null
                && causa.getMessage() != null
                && causa.getMessage().contains(texto);
    }
}
