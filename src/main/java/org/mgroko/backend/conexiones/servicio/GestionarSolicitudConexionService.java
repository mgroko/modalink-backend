package org.mgroko.backend.conexiones.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.conexiones.dto.SolicitudConexionGestionadaResponse;
import org.mgroko.backend.conexiones.exception.ConexionYaExistenteException;
import org.mgroko.backend.conexiones.exception.SolicitudConexionInvalidaException;
import org.mgroko.backend.conexiones.exception.SolicitudConexionNoEncontradaException;
import org.mgroko.backend.conexiones.exception.SolicitudConexionNoPendienteException;
import org.mgroko.backend.conexiones.exception.SolicitudConexionNoPermitidaException;
import org.mgroko.backend.modelo.Conexion;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.SolicitudConexion;
import org.mgroko.backend.modelo.enums.EstadoConexion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.servicio.PerfilActivoService;
import org.mgroko.backend.repositorio.ConexionRepository;
import org.mgroko.backend.repositorio.SolicitudConexionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-45 - Aceptar o rechazar una solicitud de conexión recibida.
 */
@Service
public class GestionarSolicitudConexionService {

    private final PerfilActivoService perfilActivoService;
    private final SolicitudConexionRepository solicitudConexionRepository;
    private final ConexionRepository conexionRepository;
    private final NotificacionService notificacionService;
    private final Clock reloj;

    public GestionarSolicitudConexionService(
            PerfilActivoService perfilActivoService,
            SolicitudConexionRepository solicitudConexionRepository,
            ConexionRepository conexionRepository,
            NotificacionService notificacionService,
            Clock reloj) {
        this.perfilActivoService = perfilActivoService;
        this.solicitudConexionRepository = solicitudConexionRepository;
        this.conexionRepository = conexionRepository;
        this.notificacionService = notificacionService;
        this.reloj = reloj;
    }

    @Transactional
    public SolicitudConexionGestionadaResponse aceptar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idSolicitudConexion) {

        Perfil perfilActivo = perfilActivoService.resolver(idUsuario, idPerfilActivo);
        SolicitudConexion solicitud = bloquearYValidar(
                idSolicitudConexion, perfilActivo);

        validarPerfilesActivos(solicitud);

        if (conexionRepository.existsActivaEntre(
                solicitud.getPerfilRemitente().getIdPerfil(),
                solicitud.getPerfilDestinatario().getIdPerfil())) {
            throw new ConexionYaExistenteException(
                    "Ya existe una conexión vigente con ese perfil.");
        }

        LocalDateTime ahora = LocalDateTime.now(reloj);
        solicitud.setEstado(EstadoSolicitud.ACEPTADA);
        solicitud.setFechaRespuesta(ahora);
        solicitud = solicitudConexionRepository.saveAndFlush(solicitud);

        Conexion conexion = crearConexion(solicitud, ahora);
        notificarAceptacion(solicitud, conexion);

        return new SolicitudConexionGestionadaResponse(
                solicitud.getIdSolicitudConexion(),
                EstadoSolicitud.ACEPTADA,
                solicitud.getFechaRespuesta(),
                conexion.getIdConexion());
    }

    @Transactional
    public SolicitudConexionGestionadaResponse rechazar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idSolicitudConexion) {

        Perfil perfilActivo = perfilActivoService.resolver(idUsuario, idPerfilActivo);
        SolicitudConexion solicitud = bloquearYValidar(
                idSolicitudConexion, perfilActivo);

        validarPerfilesActivos(solicitud);

        if (conexionRepository.existsActivaEntre(
                solicitud.getPerfilRemitente().getIdPerfil(),
                solicitud.getPerfilDestinatario().getIdPerfil())) {
            throw new ConexionYaExistenteException(
                    "Ya existe una conexión vigente con ese perfil.");
        }

        solicitud.setEstado(EstadoSolicitud.RECHAZADA);
        solicitud.setFechaRespuesta(LocalDateTime.now(reloj));
        solicitud = solicitudConexionRepository.saveAndFlush(solicitud);

        notificarRechazo(solicitud);

        return new SolicitudConexionGestionadaResponse(
                solicitud.getIdSolicitudConexion(),
                EstadoSolicitud.RECHAZADA,
                solicitud.getFechaRespuesta(),
                null);
    }

    private SolicitudConexion bloquearYValidar(
            Long idSolicitudConexion,
            Perfil perfilActivo) {

        SolicitudConexion solicitud = solicitudConexionRepository
                .findByIdForUpdate(idSolicitudConexion)
                .orElseThrow(SolicitudConexionNoEncontradaException::new);

        if (!solicitud.getPerfilDestinatario().getIdPerfil()
                .equals(perfilActivo.getIdPerfil())) {
            throw new SolicitudConexionNoPermitidaException(
                    "La solicitud no pertenece al perfil activo.");
        }

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new SolicitudConexionNoPendienteException(
                    "Sólo se pueden gestionar solicitudes de conexión pendientes.");
        }

        return solicitud;
    }

    private Conexion crearConexion(
            SolicitudConexion solicitud,
            LocalDateTime fechaInicio) {

        Perfil remitente = solicitud.getPerfilRemitente();
        Perfil destinatario = solicitud.getPerfilDestinatario();
        Perfil perfilMenor = remitente.getIdPerfil() < destinatario.getIdPerfil()
                ? remitente
                : destinatario;
        Perfil perfilMayor = remitente.getIdPerfil() < destinatario.getIdPerfil()
                ? destinatario
                : remitente;

        try {
            return conexionRepository.saveAndFlush(Conexion.builder()
                    .perfil1(perfilMenor)
                    .perfil2(perfilMayor)
                    .solicitudOrigen(solicitud)
                    .estado(EstadoConexion.ACTIVA)
                    .fechaInicioConexion(fechaInicio)
                    .build());
        } catch (DataIntegrityViolationException ex) {
            if (esConexionDuplicada(ex)) {
                throw new ConexionYaExistenteException(
                        "Ya existe una conexión vigente con ese perfil.");
            }
            throw new SolicitudConexionInvalidaException(
                    "No se pudo registrar la conexión entre los perfiles.");
        }
    }

    private void validarPerfilesActivos(SolicitudConexion solicitud) {
        validarPerfilActivo(solicitud.getPerfilRemitente(), "remitente");
        validarPerfilActivo(solicitud.getPerfilDestinatario(), "destinatario");
    }

    private void validarPerfilActivo(Perfil perfil, String rol) {
        if (perfil == null
                || perfil.getEstado() != EstadoPerfil.ACTIVO
                || perfil.getUsuario() == null
                || perfil.getUsuario().getEstado() != EstadoUsuario.ACTIVO) {
            throw new PerfilEnBajaException(
                    "El perfil " + rol + " no se encuentra activo.");
        }
    }

    private void notificarAceptacion(
            SolicitudConexion solicitud,
            Conexion conexion) {

        Perfil remitente = solicitud.getPerfilRemitente();
        Perfil destinatario = solicitud.getPerfilDestinatario();

        String mensajeRemitente = "La solicitud de conexión con '"
                + destinatario.getNombreArtistico() + "' fue aceptada.";
        String mensajeDestinatario = "Aceptaste la solicitud de conexión con '"
                + remitente.getNombreArtistico() + "'.";

        List<Notificacion> notificaciones = List.of(
                notificacionService.crear(
                        remitente.getUsuario(),
                        remitente,
                        NotificacionService.TIPO_SOLICITUD_CONEXION_ACEPTADA,
                        mensajeRemitente,
                        NotificacionService.ENTIDAD_CONEXION,
                        conexion.getIdConexion()),
                notificacionService.crear(
                        destinatario.getUsuario(),
                        destinatario,
                        NotificacionService.TIPO_SOLICITUD_CONEXION_ACEPTADA,
                        mensajeDestinatario,
                        NotificacionService.ENTIDAD_CONEXION,
                        conexion.getIdConexion()));

        notificacionService.registrarTodas(notificaciones);
    }

    private void notificarRechazo(SolicitudConexion solicitud) {
        Perfil remitente = solicitud.getPerfilRemitente();
        Perfil destinatario = solicitud.getPerfilDestinatario();

        String mensaje = "Tu solicitud de conexión con '"
                + destinatario.getNombreArtistico() + "' fue rechazada.";

        notificacionService.registrarTodas(List.of(
                notificacionService.crear(
                        remitente.getUsuario(),
                        remitente,
                        NotificacionService.TIPO_SOLICITUD_CONEXION_RECHAZADA,
                        mensaje,
                        NotificacionService.ENTIDAD_SOLICITUD_CONEXION,
                        solicitud.getIdSolicitudConexion())));
    }

    private boolean esConexionDuplicada(DataIntegrityViolationException ex) {
        Throwable causa = ex.getMostSpecificCause();
        if (causa == null || causa.getMessage() == null) {
            return false;
        }

        String mensaje = causa.getMessage();
        return mensaje.contains("UQ_conexion_activa")
                || mensaje.contains("UQ_conexion_solicitud")
                || mensaje.contains("conexión activa")
                || mensaje.contains("conexion activa");
    }
}
