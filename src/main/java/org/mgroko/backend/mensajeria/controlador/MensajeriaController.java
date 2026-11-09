package org.mgroko.backend.mensajeria.controlador;

import java.io.IOException;
import java.util.Map;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.mensajeria.config.MensajeriaProperties;
import org.mgroko.backend.mensajeria.dto.ConversacionResponse;
import org.mgroko.backend.mensajeria.dto.CrearConversacionRequest;
import org.mgroko.backend.mensajeria.dto.EnviarMensajeRequest;
import org.mgroko.backend.mensajeria.dto.MarcarLeidoRequest;
import org.mgroko.backend.mensajeria.dto.MensajeResponse;
import org.mgroko.backend.mensajeria.dto.ResumenConversacionesResponse;
import org.mgroko.backend.mensajeria.servicio.MensajeEventBus;
import org.mgroko.backend.mensajeria.servicio.MensajeriaService;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.validation.Valid;

/**
 * UC-42 - Mensajería privada.
 */
@RestController
@PreAuthorize("hasRole('USUARIO')")
public class MensajeriaController {

    private final MensajeriaService mensajeriaService;
    private final MensajeEventBus mensajeEventBus;
    private final MensajeriaProperties properties;

    public MensajeriaController(
            MensajeriaService mensajeriaService,
            MensajeEventBus mensajeEventBus,
            MensajeriaProperties properties) {
        this.mensajeriaService = mensajeriaService;
        this.mensajeEventBus = mensajeEventBus;
        this.properties = properties;
    }

    @GetMapping("/conversaciones")
    public ResponseEntity<PaginaResponse<ConversacionResponse>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        return ResponseEntity.ok(mensajeriaService.listarConversaciones(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                page,
                size));
    }

    @GetMapping("/conversaciones/resumen")
    public ResponseEntity<ResumenConversacionesResponse> resumen(
            Authentication authentication) {

        return ResponseEntity.ok(mensajeriaService.resumen(
                idUsuario(authentication),
                idPerfilActivo(authentication)));
    }

    @PostMapping("/conversaciones")
    public ResponseEntity<ConversacionResponse> crear(
            @Valid @RequestBody CrearConversacionRequest request,
            Authentication authentication) {

        var resultado = mensajeriaService.crearConversacion(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                request.idPerfilDestinatario());

        return ResponseEntity
                .status(resultado.creada() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(resultado.conversacion());
    }

    @GetMapping("/conversaciones/{idConversacion}/mensajes")
    public ResponseEntity<PaginaResponse<MensajeResponse>> mensajes(
            @PathVariable Long idConversacion,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {

        return ResponseEntity.ok(mensajeriaService.listarMensajes(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idConversacion,
                page,
                size));
    }

    @PostMapping("/conversaciones/{idConversacion}/mensajes")
    public ResponseEntity<MensajeResponse> enviar(
            @PathVariable Long idConversacion,
            @Valid @RequestBody EnviarMensajeRequest request,
            Authentication authentication) {

        return ResponseEntity.status(HttpStatus.CREATED).body(
                mensajeriaService.enviarMensaje(
                        idUsuario(authentication),
                        idPerfilActivo(authentication),
                        idConversacion,
                        request.contenido()));
    }

    @PatchMapping("/conversaciones/{idConversacion}/leido")
    public ResponseEntity<Void> marcarLeido(
            @PathVariable Long idConversacion,
            @RequestBody(required = false) MarcarLeidoRequest request,
            Authentication authentication) {

        mensajeriaService.marcarLeido(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idConversacion,
                request != null ? request.hastaIdMensaje() : null);

        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/mensajes/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> stream(Authentication authentication) {
        Long idPerfilActivo = idPerfilActivo(authentication);
        SseEmitter emitter = new SseEmitter(properties.getTimeoutStream().toMillis());

        try {
            emitter.send(SseEmitter.event()
                    .name("ready")
                    .data(Map.of("idPerfil", idPerfilActivo),
                            MediaType.APPLICATION_JSON));
        } catch (IOException ex) {
            emitter.completeWithError(ex);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(emitter);
        }

        mensajeEventBus.registrarSuscriptor(idPerfilActivo, emitter);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .header("X-Accel-Buffering", "no")
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(emitter);
    }

    private Long idUsuario(Authentication authentication) {
        return Long.parseLong((String) authentication.getPrincipal());
    }

    private Long idPerfilActivo(Authentication authentication) {
        Long idPerfilActivo = null;
        if (authentication.getDetails() instanceof ContextoAutenticacion contexto) {
            idPerfilActivo = contexto.idPerfilActivo();
        }

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }
        return idPerfilActivo;
    }
}
