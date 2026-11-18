package org.mgroko.backend.mensajeria.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.auditoria.servicio.AuditarOperacion;
import org.mgroko.backend.mensajeria.dto.ChatGrupalResponse;
import org.mgroko.backend.mensajeria.dto.MensajeGrupalEvento;
import org.mgroko.backend.mensajeria.dto.MensajeGrupalResponse;
import org.mgroko.backend.mensajeria.dto.MensajeGrupalStreamResponse;
import org.mgroko.backend.mensajeria.exception.MensajeNoPermitidoException;
import org.mgroko.backend.mensajeria.mapper.ChatGrupalMapper;
import org.mgroko.backend.modelo.MensajeProyecto;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.servicio.PerfilActivoService;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.repositorio.MensajeProyectoRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.NotificacionRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-41 - Chat grupal por proyecto.
 */
@Service
public class ChatGrupalService {

    private static final int TAMANO_MENSAJES_POR_DEFECTO = 50;
    private static final int TAMANO_MAXIMO = 100;
    private static final String CODIGO_CHAT_NO_PERMITIDO = "CHAT_GRUPAL_NO_PERMITIDO";
    private static final String CODIGO_CHAT_SOLO_LECTURA = "CHAT_GRUPAL_SOLO_LECTURA";

    private final PerfilActivoService perfilActivoService;
    private final ProyectoRepository proyectoRepository;
    private final MiembroProyectoRepository miembroProyectoRepository;
    private final MensajeProyectoRepository mensajeProyectoRepository;
    private final ProyectoSecurityService proyectoSecurityService;
    private final NotificacionService notificacionService;
    private final NotificacionRepository notificacionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock reloj;

    public ChatGrupalService(
            PerfilActivoService perfilActivoService,
            ProyectoRepository proyectoRepository,
            MiembroProyectoRepository miembroProyectoRepository,
            MensajeProyectoRepository mensajeProyectoRepository,
            ProyectoSecurityService proyectoSecurityService,
            NotificacionService notificacionService,
            NotificacionRepository notificacionRepository,
            ApplicationEventPublisher eventPublisher,
            Clock reloj) {
        this.perfilActivoService = perfilActivoService;
        this.proyectoRepository = proyectoRepository;
        this.miembroProyectoRepository = miembroProyectoRepository;
        this.mensajeProyectoRepository = mensajeProyectoRepository;
        this.proyectoSecurityService = proyectoSecurityService;
        this.notificacionService = notificacionService;
        this.notificacionRepository = notificacionRepository;
        this.eventPublisher = eventPublisher;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public ChatGrupalResponse obtenerChat(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto) {

        ContextoChat contexto = resolverContexto(idUsuario, idPerfilActivo, idProyecto);
        List<MiembroProyecto> participantes = participantesActivos(idProyecto);

        return new ChatGrupalResponse(
                contexto.proyecto().getIdProyecto(),
                contexto.proyecto().getNombre(),
                contexto.proyecto().getEstado().name(),
                permiteEnvio(contexto.proyecto()),
                participantes.stream()
                        .map(ChatGrupalMapper::toParticipante)
                        .toList());
    }

    @Transactional(readOnly = true)
    public PaginaResponse<MensajeGrupalResponse> listarMensajes(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            int page,
            int size) {

        ContextoChat contexto = resolverContexto(idUsuario, idPerfilActivo, idProyecto);

        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                normalizarTamano(size),
                Sort.by(
                        Sort.Order.desc("fechaEnvio"),
                        Sort.Order.desc("idMensajeProyecto")));

        Page<MensajeGrupalResponse> pagina = mensajeProyectoRepository
                .buscarDeProyecto(idProyecto, pageable)
                .map(mensaje -> ChatGrupalMapper.toMensajeResponse(
                        mensaje, contexto.activo().getIdPerfil()));

        return PaginaResponse.fromPage(pagina);
    }

    @Transactional
    @AuditarOperacion
    public MensajeGrupalResponse enviarMensaje(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto,
            String contenido) {

        ContextoChat contexto = resolverContexto(idUsuario, idPerfilActivo, idProyecto);

        if (!permiteEnvio(contexto.proyecto())) {
            throw new MensajeNoPermitidoException(
                    "El chat del proyecto ya no admite mensajes porque el proyecto no está Publicado ni Confirmado.",
                    CODIGO_CHAT_SOLO_LECTURA);
        }

        String texto = MensajeContenidoValidator.normalizar(contenido);
        List<MiembroProyecto> participantes = participantesActivos(idProyecto);

        MensajeProyecto mensaje = MensajeProyecto.builder()
                .proyecto(contexto.proyecto())
                .perfilEmisor(contexto.activo())
                .contenido(texto)
                .fechaEnvio(LocalDateTime.now(reloj))
                .build();

        try {
            mensaje = mensajeProyectoRepository.saveAndFlush(mensaje);
        } catch (DataIntegrityViolationException ex) {
            throw new MensajeNoPermitidoException(
                    "No se pudo enviar el mensaje. Verificá que el proyecto siga admitiendo mensajes.",
                    CODIGO_CHAT_NO_PERMITIDO);
        }

        crearNotificaciones(contexto, mensaje, participantes);

        MensajeGrupalStreamResponse stream = ChatGrupalMapper.toStreamResponse(mensaje);
        List<Long> perfilesDestinatarios = participantes.stream()
                .map(participante -> participante.getPerfil().getIdPerfil())
                .distinct()
                .toList();

        eventPublisher.publishEvent(new MensajeGrupalEvento(
                idProyecto,
                contexto.activo().getIdPerfil(),
                perfilesDestinatarios,
                stream));

        return ChatGrupalMapper.toMensajeResponse(
                mensaje, contexto.activo().getIdPerfil());
    }

    @Transactional
    public void marcarLeido(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto) {

        ContextoChat contexto = resolverContexto(idUsuario, idPerfilActivo, idProyecto);

        notificacionRepository.marcarLeidasDeChatGrupal(
                idUsuario,
                contexto.activo().getIdPerfil(),
                idProyecto,
                LocalDateTime.now(reloj));
    }

    private ContextoChat resolverContexto(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto) {

        Perfil activo = perfilActivoService.resolver(idUsuario, idPerfilActivo);

        if (activo.getUsuario() == null
                || activo.getUsuario().getEstado() != EstadoUsuario.ACTIVO) {
            throw new MensajeNoPermitidoException(
                    "El perfil no está activo para usar el chat grupal.",
                    CODIGO_CHAT_NO_PERMITIDO);
        }

        Proyecto proyecto = proyectoRepository.findById(idProyecto)
                .orElseThrow(() -> new ProyectoNoEncontradoException(idProyecto));

        proyectoSecurityService.validarPermiso(
                idProyecto,
                activo.getIdPerfil(),
                ProyectoSecurityService.PERMISO_VER_PROYECTO);

        return new ContextoChat(activo, proyecto);
    }

    private List<MiembroProyecto> participantesActivos(Long idProyecto) {
        return miembroProyectoRepository.buscarActivosDeProyectoParaChat(idProyecto)
                .stream()
                .filter(miembro -> miembro.getPerfil() != null)
                .filter(miembro -> miembro.getPerfil().getEstado() == EstadoPerfil.ACTIVO)
                .filter(miembro -> miembro.getPerfil().getUsuario() != null)
                .filter(miembro -> miembro.getPerfil().getUsuario().getEstado()
                        == EstadoUsuario.ACTIVO)
                .toList();
    }

    private void crearNotificaciones(
            ContextoChat contexto,
            MensajeProyecto mensaje,
            List<MiembroProyecto> participantes) {

        Set<Long> perfilesConNotificacionNoLeida = new HashSet<>(
                notificacionRepository.buscarPerfilesConNotificacionNoLeida(
                        NotificacionService.TIPO_MENSAJE_GRUPAL_RECIBIDO,
                        contexto.proyecto().getIdProyecto()));

        Long idUsuarioEmisor = contexto.activo().getUsuario().getIdUsuario();
        String texto = "Tenés un mensaje nuevo en el chat de '"
                + contexto.proyecto().getNombre() + "'.";

        List<Notificacion> nuevas = participantes.stream()
                .map(MiembroProyecto::getPerfil)
                .filter(perfil -> !perfil.getIdPerfil()
                        .equals(contexto.activo().getIdPerfil()))
                .filter(perfil -> !perfil.getUsuario().getIdUsuario()
                        .equals(idUsuarioEmisor))
                .filter(perfil -> !perfilesConNotificacionNoLeida
                        .contains(perfil.getIdPerfil()))
                .map(perfil -> notificacionService.crear(
                        perfil.getUsuario(),
                        perfil,
                        NotificacionService.TIPO_MENSAJE_GRUPAL_RECIBIDO,
                        texto,
                        NotificacionService.ENTIDAD_PROYECTO,
                        contexto.proyecto().getIdProyecto(),
                        contexto.proyecto().getIdProyecto()))
                .toList();

        notificacionService.registrarTodas(nuevas);
    }

    private boolean permiteEnvio(Proyecto proyecto) {
        return proyecto.getEstado() == EstadoProyecto.PUBLICADO
                || proyecto.getEstado() == EstadoProyecto.CONFIRMADO;
    }

    private int normalizarTamano(int size) {
        if (size <= 0) {
            return TAMANO_MENSAJES_POR_DEFECTO;
        }
        return Math.min(size, TAMANO_MAXIMO);
    }

    private record ContextoChat(Perfil activo, Proyecto proyecto) {
    }
}
