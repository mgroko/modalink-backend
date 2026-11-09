package org.mgroko.backend.conexiones.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.mgroko.backend.auth.exception.UsuarioNoEncontradoException;
import org.mgroko.backend.conexiones.dto.EnviarSolicitudConexionRequest;
import org.mgroko.backend.conexiones.dto.SolicitudConexionResponse;
import org.mgroko.backend.conexiones.exception.ConexionYaExistenteException;
import org.mgroko.backend.conexiones.exception.SolicitudConexionDuplicadaException;
import org.mgroko.backend.conexiones.exception.SolicitudConexionInvalidaException;
import org.mgroko.backend.conexiones.mapper.SolicitudConexionMapper;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.SolicitudConexion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.repositorio.ConexionRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.SolicitudConexionRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mgroko.backend.security.PerfilAccessPolicy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-43 - Enviar solicitud de conexión.
 */
@Service
public class EnviarSolicitudConexionService {

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final SolicitudConexionRepository solicitudConexionRepository;
    private final ConexionRepository conexionRepository;
    private final NotificacionService notificacionService;
    private final Clock reloj;

    public EnviarSolicitudConexionService(
            UsuarioRepository usuarioRepository,
            PerfilRepository perfilRepository,
            SolicitudConexionRepository solicitudConexionRepository,
            ConexionRepository conexionRepository,
            NotificacionService notificacionService,
            Clock reloj) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.solicitudConexionRepository = solicitudConexionRepository;
        this.conexionRepository = conexionRepository;
        this.notificacionService = notificacionService;
        this.reloj = reloj;
    }

    @Transactional
    public SolicitudConexionResponse enviar(
            Long idUsuario,
            Long idPerfilActivo,
            EnviarSolicitudConexionRequest request) {

        Perfil remitente = resolverPerfilActivo(idUsuario, idPerfilActivo);
        Perfil destinatario = resolverDestinatario(request.idPerfilDestinatario());

        validarPerfilesDistintos(remitente, destinatario);

        if (conexionRepository.existsActivaEntre(
                remitente.getIdPerfil(), destinatario.getIdPerfil())) {
            throw new ConexionYaExistenteException(
                    "Ya existe una conexión vigente con ese perfil.");
        }

        if (solicitudConexionRepository.existsPendienteEntre(
                remitente.getIdPerfil(), destinatario.getIdPerfil())) {
            throw new SolicitudConexionDuplicadaException(
                    "Ya existe una solicitud de conexión pendiente con ese perfil.");
        }

        SolicitudConexion solicitud = SolicitudConexion.builder()
                .perfilRemitente(remitente)
                .perfilDestinatario(destinatario)
                .estado(EstadoSolicitud.PENDIENTE)
                .fechaEnvio(LocalDateTime.now(reloj))
                .build();

        try {
            solicitud = solicitudConexionRepository.saveAndFlush(solicitud);
        } catch (DataIntegrityViolationException ex) {
            if (esSolicitudPendienteDuplicada(ex)) {
                throw new SolicitudConexionDuplicadaException(
                        "Ya existe una solicitud de conexión pendiente con ese perfil.");
            }
            if (esConexionActiva(ex)) {
                throw new ConexionYaExistenteException(
                        "Ya existe una conexión vigente con ese perfil.");
            }
            throw new SolicitudConexionInvalidaException(
                    "No se pudo registrar la solicitud de conexión.");
        }

        notificarSolicitud(destinatario, remitente, solicitud);
        return SolicitudConexionMapper.toResponse(solicitud);
    }

    private Perfil resolverPerfilActivo(Long idUsuario, Long idPerfilActivo) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado."));

        if (!usuario.getEstado().permiteAcceso()) {
            throw new UsuarioNoEncontradoException("Usuario no encontrado.");
        }

        PerfilAccessPolicy.validarNoAdministrador(usuario);

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        Perfil perfil = perfilRepository
                .findByIdPerfilAndUsuarioIdUsuario(idPerfilActivo, idUsuario)
                .orElseThrow(PerfilNoEncontradoException::new);

        if (perfil.getEstado() != EstadoPerfil.ACTIVO) {
            throw new PerfilEnBajaException("El perfil no se encuentra activo.");
        }

        if (perfil.getUsuario() == null
                || perfil.getUsuario().getEstado() != EstadoUsuario.ACTIVO) {
            throw new PerfilEnBajaException(
                    "El usuario del perfil no se encuentra activo.");
        }

        return perfil;
    }

    private Perfil resolverDestinatario(Long idPerfilDestinatario) {
        Perfil destinatario = perfilRepository.findById(idPerfilDestinatario)
                .orElseThrow(PerfilNoEncontradoException::new);

        // Mismas reglas de visibilidad que UC-14 para perfiles de terceros.
        if (destinatario.getEstado() != EstadoPerfil.ACTIVO
                || destinatario.getUsuario() == null
                || destinatario.getUsuario().getEstado() != EstadoUsuario.ACTIVO) {
            throw new PerfilNoEncontradoException();
        }

        return destinatario;
    }

    private void validarPerfilesDistintos(Perfil remitente, Perfil destinatario) {
        if (remitente.getIdPerfil().equals(destinatario.getIdPerfil())
                || remitente.getUsuario() == null
                || destinatario.getUsuario() == null
                || Objects.equals(
                        remitente.getUsuario().getIdUsuario(),
                        destinatario.getUsuario().getIdUsuario())) {
            throw new SolicitudConexionInvalidaException(
                    "No se puede enviar una solicitud entre perfiles del mismo usuario.");
        }
    }

    private void notificarSolicitud(
            Perfil destinatario,
            Perfil remitente,
            SolicitudConexion solicitud) {

        String mensaje = "El perfil '" + remitente.getNombreArtistico()
                + "' te envió una solicitud de conexión.";

        notificacionService.registrarTodas(List.of(
                notificacionService.crear(
                        destinatario.getUsuario(),
                        destinatario,
                        NotificacionService.TIPO_SOLICITUD_CONEXION_RECIBIDA,
                        mensaje,
                        NotificacionService.ENTIDAD_SOLICITUD_CONEXION,
                        solicitud.getIdSolicitudConexion())));
    }

    private boolean esSolicitudPendienteDuplicada(DataIntegrityViolationException ex) {
        return mensajeContiene(ex, "UQ_solicitud_conexion_pendiente");
    }

    private boolean esConexionActiva(DataIntegrityViolationException ex) {
        return mensajeContiene(ex, "conexión activa")
                || mensajeContiene(ex, "conexion activa");
    }

    private boolean mensajeContiene(DataIntegrityViolationException ex, String texto) {
        Throwable causa = ex.getMostSpecificCause();
        return causa != null
                && causa.getMessage() != null
                && causa.getMessage().contains(texto);
    }
}
