package org.mgroko.backend.publicaciones.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;

import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.SolicitudColaboracion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.servicio.PerfilActivoService;
import org.mgroko.backend.publicaciones.dto.SolicitudColaboracionGestionadaResponse;
import org.mgroko.backend.publicaciones.exception.SolicitudColaboracionInvalidaException;
import org.mgroko.backend.publicaciones.exception.SolicitudColaboracionNoEncontradaException;
import org.mgroko.backend.publicaciones.exception.SolicitudColaboracionNoPendienteException;
import org.mgroko.backend.publicaciones.exception.SolicitudColaboracionNoPermitidaException;
import org.mgroko.backend.publicaciones.mapper.SolicitudColaboracionMapper;
import org.mgroko.backend.repositorio.PublicacionRepository;
import org.mgroko.backend.repositorio.SolicitudColaboracionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-62/63 - Aceptar o rechazar una solicitud de colaboración.
 */
@Service
public class GestionarSolicitudColaboracionService {

    private final PerfilActivoService perfilActivoService;
    private final SolicitudColaboracionRepository solicitudColaboracionRepository;
    private final PublicacionRepository publicacionRepository;
    private final NotificacionService notificacionService;
    private final Clock reloj;

    public GestionarSolicitudColaboracionService(
            PerfilActivoService perfilActivoService,
            SolicitudColaboracionRepository solicitudColaboracionRepository,
            PublicacionRepository publicacionRepository,
            NotificacionService notificacionService,
            Clock reloj) {
        this.perfilActivoService = perfilActivoService;
        this.solicitudColaboracionRepository = solicitudColaboracionRepository;
        this.publicacionRepository = publicacionRepository;
        this.notificacionService = notificacionService;
        this.reloj = reloj;
    }

    @Transactional
    public SolicitudColaboracionGestionadaResponse aceptar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idSolicitudColaboracion) {

        Perfil perfilActivo = perfilActivoService.resolver(idUsuario, idPerfilActivo);
        SolicitudColaboracion solicitud = bloquearYValidar(
                idSolicitudColaboracion, perfilActivo);

        Publicacion publicacion = solicitud.getPublicacion();
        validarPublicacionDisponible(publicacion);

        if (publicacionRepository.existsColaborador(
                publicacion.getIdPublicacion(), perfilActivo.getIdPerfil())) {
            throw new SolicitudColaboracionInvalidaException(
                    "El perfil destinatario ya es colaborador de la publicación.",
                    "DESTINATARIO_YA_ES_COLABORADOR");
        }

        if (publicacion.getColaboradores() == null) {
            publicacion.setColaboradores(new LinkedHashSet<>());
        }

        boolean agregado = publicacion.getColaboradores().add(perfilActivo);
        if (!agregado) {
            throw new SolicitudColaboracionInvalidaException(
                    "El perfil destinatario ya es colaborador de la publicación.",
                    "DESTINATARIO_YA_ES_COLABORADOR");
        }

        try {
            publicacionRepository.saveAndFlush(publicacion);
        } catch (DataIntegrityViolationException ex) {
            if (mensajeContiene(ex, "PK_colaborador_publicacion")) {
                throw new SolicitudColaboracionInvalidaException(
                        "El perfil destinatario ya es colaborador de la publicación.",
                        "DESTINATARIO_YA_ES_COLABORADOR");
            }
            throw ex;
        }

        solicitud.setEstado(EstadoSolicitud.ACEPTADA);
        solicitud.setFechaRespuesta(LocalDateTime.now(reloj));
        solicitud = solicitudColaboracionRepository.saveAndFlush(solicitud);

        notificarRespuesta(
                solicitud,
                NotificacionService.TIPO_SOLICITUD_COLABORACION_ACEPTADA,
                "aceptó");

        return SolicitudColaboracionMapper.toGestionadaResponse(solicitud, true);
    }

    @Transactional
    public SolicitudColaboracionGestionadaResponse rechazar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idSolicitudColaboracion) {

        Perfil perfilActivo = perfilActivoService.resolver(idUsuario, idPerfilActivo);
        SolicitudColaboracion solicitud = bloquearYValidar(
                idSolicitudColaboracion, perfilActivo);

        solicitud.setEstado(EstadoSolicitud.RECHAZADA);
        solicitud.setFechaRespuesta(LocalDateTime.now(reloj));
        solicitud = solicitudColaboracionRepository.saveAndFlush(solicitud);

        notificarRespuesta(
                solicitud,
                NotificacionService.TIPO_SOLICITUD_COLABORACION_RECHAZADA,
                "rechazó");

        return SolicitudColaboracionMapper.toGestionadaResponse(solicitud, false);
    }

    private SolicitudColaboracion bloquearYValidar(
            Long idSolicitudColaboracion,
            Perfil perfilActivo) {

        SolicitudColaboracion solicitud = solicitudColaboracionRepository
                .findByIdForUpdate(idSolicitudColaboracion)
                .orElseThrow(SolicitudColaboracionNoEncontradaException::new);

        if (!solicitud.getPerfilDestinatario().getIdPerfil()
                .equals(perfilActivo.getIdPerfil())) {
            throw new SolicitudColaboracionNoPermitidaException(
                    "Solo el perfil destinatario puede gestionar la solicitud.");
        }

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new SolicitudColaboracionNoPendienteException(
                    "La solicitud de colaboración ya fue gestionada.");
        }

        return solicitud;
    }

    private void validarPublicacionDisponible(Publicacion publicacion) {
        Perfil remitente = publicacion.getPerfil();

        if (publicacion.getEstado() != EstadoPublicacion.ACTIVA
                || remitente == null
                || remitente.getEstado() != EstadoPerfil.ACTIVO
                || remitente.getUsuario() == null
                || remitente.getUsuario().getEstado() != EstadoUsuario.ACTIVO) {
            throw new SolicitudColaboracionInvalidaException(
                    "La publicación ya no está disponible para colaborar.",
                    "PUBLICACION_NO_DISPONIBLE");
        }
    }

    private void notificarRespuesta(
            SolicitudColaboracion solicitud,
            String tipo,
            String accion) {

        Perfil remitente = solicitud.getPerfilRemitente();
        String mensaje = "El perfil '" + solicitud.getPerfilDestinatario().getNombreArtistico()
                + "' " + accion
                + " tu solicitud para colaborar en la publicación '"
                + solicitud.getPublicacion().getTitulo() + "'.";

        notificacionService.registrarTodas(List.of(
                notificacionService.crear(
                        remitente.getUsuario(),
                        remitente,
                        tipo,
                        mensaje,
                        NotificacionService.ENTIDAD_PUBLICACION,
                        solicitud.getPublicacion().getIdPublicacion())));
    }

    private boolean mensajeContiene(DataIntegrityViolationException ex, String texto) {
        Throwable causa = ex.getMostSpecificCause();
        return causa != null
                && causa.getMessage() != null
                && causa.getMessage().contains(texto);
    }
}
