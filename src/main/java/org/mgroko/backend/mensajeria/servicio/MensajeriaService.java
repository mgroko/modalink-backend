package org.mgroko.backend.mensajeria.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.mgroko.backend.admin.exception.PerfilNoEncontradoException;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.mensajeria.dto.ConversacionLeidaEvento;
import org.mgroko.backend.mensajeria.dto.ConversacionResponse;
import org.mgroko.backend.mensajeria.dto.MensajeEvento;
import org.mgroko.backend.mensajeria.dto.MensajeResponse;
import org.mgroko.backend.mensajeria.dto.MensajeStreamResponse;
import org.mgroko.backend.mensajeria.dto.ResumenConversacionesResponse;
import org.mgroko.backend.mensajeria.dto.UltimoMensajeRow;
import org.mgroko.backend.mensajeria.exception.ConversacionNoEncontradaException;
import org.mgroko.backend.mensajeria.exception.ConversacionNoPermitidaException;
import org.mgroko.backend.mensajeria.exception.MensajeInvalidoException;
import org.mgroko.backend.mensajeria.exception.MensajeNoPermitidoException;
import org.mgroko.backend.mensajeria.mapper.MensajeriaMapper;
import org.mgroko.backend.modelo.Conversacion;
import org.mgroko.backend.modelo.Mensaje;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.servicio.PerfilActivoService;
import org.mgroko.backend.repositorio.ConexionRepository;
import org.mgroko.backend.repositorio.ConversacionRepository;
import org.mgroko.backend.repositorio.MensajeRepository;
import org.mgroko.backend.repositorio.NotificacionRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-42 - Mensajería privada entre perfiles con conexión activa.
 */
@Service
public class MensajeriaService {

    private static final int TAMANO_CONVERSACIONES_POR_DEFECTO = 20;
    private static final int TAMANO_MENSAJES_POR_DEFECTO = 50;
    private static final int TAMANO_MAXIMO = 100;
    private static final int MAX_CONTENIDO = 1000;

    private final PerfilActivoService perfilActivoService;
    private final PerfilRepository perfilRepository;
    private final ConexionRepository conexionRepository;
    private final ConversacionRepository conversacionRepository;
    private final MensajeRepository mensajeRepository;
    private final NotificacionService notificacionService;
    private final NotificacionRepository notificacionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock reloj;

    public MensajeriaService(
            PerfilActivoService perfilActivoService,
            PerfilRepository perfilRepository,
            ConexionRepository conexionRepository,
            ConversacionRepository conversacionRepository,
            MensajeRepository mensajeRepository,
            NotificacionService notificacionService,
            NotificacionRepository notificacionRepository,
            ApplicationEventPublisher eventPublisher,
            Clock reloj) {
        this.perfilActivoService = perfilActivoService;
        this.perfilRepository = perfilRepository;
        this.conexionRepository = conexionRepository;
        this.conversacionRepository = conversacionRepository;
        this.mensajeRepository = mensajeRepository;
        this.notificacionService = notificacionService;
        this.notificacionRepository = notificacionRepository;
        this.eventPublisher = eventPublisher;
        this.reloj = reloj;
    }

    @Transactional
    public ResultadoConversacion crearConversacion(
            Long idUsuario,
            Long idPerfilActivo,
            Long idPerfilDestinatario) {

        Perfil activo = perfilActivoService.resolver(idUsuario, idPerfilActivo);
        Perfil destinatario = validarDestinatario(activo, idPerfilDestinatario);

        Long idPerfil1 = Math.min(activo.getIdPerfil(), destinatario.getIdPerfil());
        Long idPerfil2 = Math.max(activo.getIdPerfil(), destinatario.getIdPerfil());

        var existente = conversacionRepository.buscarPorPar(idPerfil1, idPerfil2);
        if (existente.isPresent()) {
            return new ResultadoConversacion(
                    toConversacionResponse(
                            existente.get(), activo.getIdPerfil(), true),
                    false);
        }

        Conversacion nueva = Conversacion.builder()
                .perfil1(perfilRepository.getReferenceById(idPerfil1))
                .perfil2(perfilRepository.getReferenceById(idPerfil2))
                .fechaCreacion(LocalDateTime.now(reloj))
                .build();

        try {
            Conversacion guardada = conversacionRepository.saveAndFlush(nueva);
            return new ResultadoConversacion(
                    toConversacionResponse(guardada, activo.getIdPerfil(), true),
                    true);
        } catch (DataIntegrityViolationException ex) {
            Conversacion existenteTrasCarrera = conversacionRepository
                    .buscarPorPar(idPerfil1, idPerfil2)
                    .orElseThrow(() -> new ConversacionNoPermitidaException(
                            "No se pudo iniciar la conversación. Verificá que la conexión siga activa."));
            return new ResultadoConversacion(
                    toConversacionResponse(
                            existenteTrasCarrera, activo.getIdPerfil(), true),
                    false);
        }
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ConversacionResponse> listarConversaciones(
            Long idUsuario,
            Long idPerfilActivo,
            int page,
            int size) {

        Perfil activo = perfilActivoService.resolver(idUsuario, idPerfilActivo);

        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                normalizarTamano(size, TAMANO_CONVERSACIONES_POR_DEFECTO),
                Sort.by(
                        Sort.Order.desc("fechaUltimoMensaje"),
                        Sort.Order.desc("idConversacion")));

        Page<Conversacion> pagina = conversacionRepository.buscarDePerfil(
                activo.getIdPerfil(), pageable);

        List<Long> ids = pagina.getContent().stream()
                .map(Conversacion::getIdConversacion)
                .toList();

        if (ids.isEmpty()) {
            return PaginaResponse.fromPage(
                    new PageImpl<>(List.of(), pageable, pagina.getTotalElements()));
        }

        Map<Long, UltimoMensajeRow> ultimos = mensajeRepository
                .buscarUltimosPorConversaciones(ids)
                .stream()
                .collect(Collectors.toMap(
                        UltimoMensajeRow::idConversacion,
                        fila -> fila));

        Set<Long> noLeidos = new HashSet<>(
                mensajeRepository.buscarIdsConNoLeidos(ids, activo.getIdPerfil()));

        Set<Long> contrapartesActivas = new HashSet<>(
                conexionRepository.buscarIdsContrapartesActivas(activo.getIdPerfil()));

        List<ConversacionResponse> contenido = pagina.getContent().stream()
                .map(conversacion -> MensajeriaMapper.toConversacionResponse(
                        conversacion,
                        activo.getIdPerfil(),
                        ultimos.get(conversacion.getIdConversacion()),
                        noLeidos.contains(conversacion.getIdConversacion()),
                        perfilesActivos(conversacion)
                                && contrapartesActivas.contains(
                                        MensajeriaMapper.contraparte(
                                                conversacion, activo.getIdPerfil())
                                                .getIdPerfil())))
                .toList();

        return PaginaResponse.fromPage(
                new PageImpl<>(contenido, pageable, pagina.getTotalElements()));
    }

    @Transactional(readOnly = true)
    public ResumenConversacionesResponse resumen(
            Long idUsuario,
            Long idPerfilActivo) {

        Perfil activo = perfilActivoService.resolver(idUsuario, idPerfilActivo);

        return new ResumenConversacionesResponse(
                conversacionRepository.contarDePerfil(activo.getIdPerfil()),
                mensajeRepository.contarConversacionesNoLeidas(activo.getIdPerfil()));
    }

    @Transactional(readOnly = true)
    public PaginaResponse<MensajeResponse> listarMensajes(
            Long idUsuario,
            Long idPerfilActivo,
            Long idConversacion,
            int page,
            int size) {

        Perfil activo = perfilActivoService.resolver(idUsuario, idPerfilActivo);
        Conversacion conversacion = conversacionRepository
                .buscarPorIdConPerfiles(idConversacion)
                .orElseThrow(ConversacionNoEncontradaException::new);

        if (!participa(conversacion, activo.getIdPerfil())) {
            throw new ConversacionNoEncontradaException();
        }

        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                normalizarTamano(size, TAMANO_MENSAJES_POR_DEFECTO),
                Sort.by(
                        Sort.Order.desc("fechaEnvio"),
                        Sort.Order.desc("idMensaje")));

        Page<Mensaje> pagina = mensajeRepository.buscarDeConversacion(
                idConversacion, pageable);

        List<MensajeResponse> contenido = pagina.getContent().stream()
                .map(mensaje -> MensajeriaMapper.toMensajeResponse(
                        mensaje, activo.getIdPerfil()))
                .toList();

        return PaginaResponse.fromPage(
                new PageImpl<>(contenido, pageable, pagina.getTotalElements()));
    }

    @Transactional
    public MensajeResponse enviarMensaje(
            Long idUsuario,
            Long idPerfilActivo,
            Long idConversacion,
            String contenido) {

        Perfil activo = perfilActivoService.resolver(idUsuario, idPerfilActivo);
        String texto = normalizarContenido(contenido);

        Conversacion conversacion = conversacionRepository
                .findByIdForUpdate(idConversacion)
                .orElseThrow(ConversacionNoEncontradaException::new);

        if (!participa(conversacion, activo.getIdPerfil())) {
            throw new ConversacionNoEncontradaException();
        }

        Perfil destinatario = MensajeriaMapper.contraparte(
                conversacion, activo.getIdPerfil());

        validarConexionActiva(conversacion);
        validarPerfilActivo(destinatario);

        boolean yaHabiaNoLeidos = mensajeRepository.existeNoLeidoDeContraparte(
                idConversacion, activo.getIdPerfil());

        Mensaje mensaje = Mensaje.builder()
                .conversacion(conversacion)
                .perfilEmisor(activo)
                .contenido(texto)
                .fechaEnvio(LocalDateTime.now(reloj))
                .leido(false)
                .fechaLectura(null)
                .build();

        try {
            mensaje = mensajeRepository.saveAndFlush(mensaje);
        } catch (DataIntegrityViolationException ex) {
            throw new MensajeNoPermitidoException(
                    "No se pudo enviar el mensaje. Verificá que la conexión siga activa.");
        }

        crearNotificacionSiCorresponde(
                conversacion, activo, destinatario, yaHabiaNoLeidos);

        MensajeResponse response = MensajeriaMapper.toMensajeResponse(
                mensaje, activo.getIdPerfil());
        MensajeStreamResponse stream = MensajeriaMapper.toMensajeStreamResponse(mensaje);

        eventPublisher.publishEvent(new MensajeEvento(
                conversacion.getIdConversacion(),
                activo.getIdPerfil(),
                destinatario.getIdPerfil(),
                stream));

        return response;
    }

    @Transactional
    public void marcarLeido(
            Long idUsuario,
            Long idPerfilActivo,
            Long idConversacion,
            Long hastaIdMensaje) {

        Perfil activo = perfilActivoService.resolver(idUsuario, idPerfilActivo);

        Conversacion conversacion = conversacionRepository
                .findByIdForUpdate(idConversacion)
                .orElseThrow(ConversacionNoEncontradaException::new);

        if (!participa(conversacion, activo.getIdPerfil())) {
            throw new ConversacionNoEncontradaException();
        }

        Perfil contraparte = MensajeriaMapper.contraparte(
                conversacion, activo.getIdPerfil());

        if (hastaIdMensaje != null) {
            Mensaje hasta = mensajeRepository
                    .findByIdMensajeAndConversacion_IdConversacion(
                            hastaIdMensaje, idConversacion)
                    .orElseThrow(() -> new MensajeInvalidoException(
                            "El mensaje indicado no pertenece a la conversación."));

            if (hasta.getPerfilEmisor().getIdPerfil().equals(activo.getIdPerfil())) {
                throw new MensajeInvalidoException(
                        "El mensaje indicado no es de la contraparte.");
            }
        }

        LocalDateTime fechaLectura = LocalDateTime.now(reloj);
        int actualizados = mensajeRepository.marcarLeidos(
                idConversacion,
                activo.getIdPerfil(),
                hastaIdMensaje,
                fechaLectura);

        boolean quedanNoLeidos = mensajeRepository.existeNoLeidoDeContraparte(
                idConversacion, activo.getIdPerfil());

        if (!quedanNoLeidos) {
            notificacionRepository.marcarLeidasDeConversacion(
                    idUsuario, idConversacion, fechaLectura);
        }

        Long ultimoId = hastaIdMensaje != null
                ? hastaIdMensaje
                : mensajeRepository.buscarUltimoIdDeContraparte(
                        idConversacion, activo.getIdPerfil())
                        .orElse(null);

        if (actualizados > 0 && ultimoId != null) {
            eventPublisher.publishEvent(new ConversacionLeidaEvento(
                    idConversacion,
                    activo.getIdPerfil(),
                    contraparte.getIdPerfil(),
                    ultimoId,
                    fechaLectura));
        }
    }

    private Perfil validarDestinatario(Perfil activo, Long idPerfilDestinatario) {
        if (idPerfilDestinatario == null) {
            throw new PerfilNoEncontradoException("Perfil no encontrado.");
        }

        if (idPerfilDestinatario.equals(activo.getIdPerfil())) {
            throw new ConversacionNoPermitidaException(
                    "No podés iniciar una conversación con tu propio perfil.");
        }

        Perfil destinatario = perfilRepository.findById(idPerfilDestinatario)
                .orElseThrow(() -> new PerfilNoEncontradoException(
                        "Perfil no encontrado."));

        validarPerfilActivo(destinatario);

        if (destinatario.getUsuario().getIdUsuario()
                .equals(activo.getUsuario().getIdUsuario())) {
            throw new ConversacionNoPermitidaException(
                    "No podés chatear entre perfiles del mismo usuario.");
        }

        if (!conexionRepository.existsActivaEntre(
                activo.getIdPerfil(), destinatario.getIdPerfil())) {
            throw new ConversacionNoPermitidaException(
                    "Solo podés chatear con perfiles que tengan una conexión activa.");
        }

        return destinatario;
    }

    private void validarPerfilActivo(Perfil perfil) {
        if (perfil == null
                || perfil.getEstado() != EstadoPerfil.ACTIVO
                || perfil.getUsuario() == null
                || perfil.getUsuario().getEstado() != EstadoUsuario.ACTIVO) {
            throw new ConversacionNoPermitidaException(
                    "El perfil destinatario no está activo.");
        }
    }

    private void validarConexionActiva(Conversacion conversacion) {
        if (!conexionRepository.existsActivaEntre(
                conversacion.getPerfil1().getIdPerfil(),
                conversacion.getPerfil2().getIdPerfil())) {
            throw new MensajeNoPermitidoException(
                    "La conexión entre los perfiles ya no está activa.");
        }
    }

    private boolean participa(Conversacion conversacion, Long idPerfilActivo) {
        return conversacion.getPerfil1().getIdPerfil().equals(idPerfilActivo)
                || conversacion.getPerfil2().getIdPerfil().equals(idPerfilActivo);
    }

    private boolean perfilesActivos(Conversacion conversacion) {
        Perfil p1 = conversacion.getPerfil1();
        Perfil p2 = conversacion.getPerfil2();

        return p1.getEstado() == EstadoPerfil.ACTIVO
                && p2.getEstado() == EstadoPerfil.ACTIVO
                && p1.getUsuario() != null
                && p2.getUsuario() != null
                && p1.getUsuario().getEstado() == EstadoUsuario.ACTIVO
                && p2.getUsuario().getEstado() == EstadoUsuario.ACTIVO;
    }

    private ConversacionResponse toConversacionResponse(
            Conversacion conversacion,
            Long idPerfilActivo,
            boolean recienCreada) {

        UltimoMensajeRow ultimo = mensajeRepository
                .buscarUltimosPorConversaciones(
                        List.of(conversacion.getIdConversacion()))
                .stream()
                .findFirst()
                .orElse(null);

        boolean tieneNoLeidos = !mensajeRepository
                .buscarIdsConNoLeidos(
                        List.of(conversacion.getIdConversacion()),
                        idPerfilActivo)
                .isEmpty();

        boolean puedeEnviar = recienCreada
                ? true
                : perfilesActivos(conversacion)
                        && conexionRepository.existsActivaEntre(
                                conversacion.getPerfil1().getIdPerfil(),
                                conversacion.getPerfil2().getIdPerfil());

        return MensajeriaMapper.toConversacionResponse(
                conversacion,
                idPerfilActivo,
                ultimo,
                tieneNoLeidos,
                puedeEnviar);
    }

    private void crearNotificacionSiCorresponde(
            Conversacion conversacion,
            Perfil emisor,
            Perfil destinatario,
            boolean yaHabiaNoLeidos) {

        if (yaHabiaNoLeidos) {
            return;
        }

        boolean existeNoLeida = notificacionRepository
                .existsByUsuarioDestino_IdUsuarioAndTipoAndEntidadTipoAndEntidadIdAndLeidaFalse(
                        destinatario.getUsuario().getIdUsuario(),
                        NotificacionService.TIPO_MENSAJE_PRIVADO_RECIBIDO,
                        NotificacionService.ENTIDAD_CONVERSACION,
                        conversacion.getIdConversacion());

        if (existeNoLeida) {
            return;
        }

        Notificacion notificacion = notificacionService.crear(
                destinatario.getUsuario(),
                destinatario,
                NotificacionService.TIPO_MENSAJE_PRIVADO_RECIBIDO,
                "Tenés mensajes sin leer de '" + emisor.getNombreArtistico() + "'.",
                NotificacionService.ENTIDAD_CONVERSACION,
                conversacion.getIdConversacion());

        notificacionService.registrarTodas(List.of(notificacion));
    }

    private String normalizarContenido(String contenido) {
        String texto = contenido == null ? "" : contenido.trim();

        if (texto.isEmpty()) {
            throw new MensajeInvalidoException("El mensaje no puede estar vacío.");
        }

        if (texto.length() > MAX_CONTENIDO) {
            throw new MensajeInvalidoException(
                    "El mensaje no puede superar los 1000 caracteres.");
        }

        return texto;
    }

    private int normalizarTamano(int size, int porDefecto) {
        if (size <= 0) {
            return porDefecto;
        }
        return Math.min(size, TAMANO_MAXIMO);
    }

    public record ResultadoConversacion(
            ConversacionResponse conversacion,
            boolean creada) {
    }
}
