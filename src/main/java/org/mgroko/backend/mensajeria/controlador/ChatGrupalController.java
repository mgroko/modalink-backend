package org.mgroko.backend.mensajeria.controlador;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.mensajeria.dto.ChatGrupalResponse;
import org.mgroko.backend.mensajeria.dto.EnviarMensajeRequest;
import org.mgroko.backend.mensajeria.dto.MensajeGrupalResponse;
import org.mgroko.backend.mensajeria.servicio.ChatGrupalService;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.http.HttpStatus;
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

import jakarta.validation.Valid;

/**
 * UC-41 - Chat grupal del proyecto.
 */
@RestController
@PreAuthorize("hasRole('USUARIO')")
public class ChatGrupalController {

    private final ChatGrupalService chatGrupalService;

    public ChatGrupalController(ChatGrupalService chatGrupalService) {
        this.chatGrupalService = chatGrupalService;
    }

    @GetMapping("/proyectos/{idProyecto}/chat")
    public ResponseEntity<ChatGrupalResponse> obtener(
            @PathVariable Long idProyecto,
            Authentication authentication) {

        return ResponseEntity.ok(chatGrupalService.obtenerChat(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idProyecto));
    }

    @GetMapping("/proyectos/{idProyecto}/chat/mensajes")
    public ResponseEntity<PaginaResponse<MensajeGrupalResponse>> mensajes(
            @PathVariable Long idProyecto,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {

        return ResponseEntity.ok(chatGrupalService.listarMensajes(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idProyecto,
                page,
                size));
    }

    @PostMapping("/proyectos/{idProyecto}/chat/mensajes")
    public ResponseEntity<MensajeGrupalResponse> enviar(
            @PathVariable Long idProyecto,
            @Valid @RequestBody EnviarMensajeRequest request,
            Authentication authentication) {

        return ResponseEntity.status(HttpStatus.CREATED).body(
                chatGrupalService.enviarMensaje(
                        idUsuario(authentication),
                        idPerfilActivo(authentication),
                        idProyecto,
                        request.contenido()));
    }

    @PatchMapping("/proyectos/{idProyecto}/chat/leido")
    public ResponseEntity<Void> marcarLeido(
            @PathVariable Long idProyecto,
            Authentication authentication) {

        chatGrupalService.marcarLeido(
                idUsuario(authentication),
                idPerfilActivo(authentication),
                idProyecto);

        return ResponseEntity.noContent().build();
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
