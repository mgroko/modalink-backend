package org.mgroko.backend.notificaciones.controlador;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.notificaciones.dto.NotificacionMarcadoResponse;
import org.mgroko.backend.notificaciones.dto.NotificacionResponse;
import org.mgroko.backend.notificaciones.dto.NotificacionResumenResponse;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @GetMapping
    public ResponseEntity<PaginaResponse<NotificacionResponse>> listar(
            @RequestParam(required = false) Long idPerfilContexto,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        Long idUsuario = idUsuario(authentication);
        return ResponseEntity.ok(notificacionService.listar(idUsuario, idPerfilContexto, page, size));
    }

    @GetMapping("/resumen")
    public ResponseEntity<NotificacionResumenResponse> resumen(
            @RequestParam(required = false) Long idPerfilContexto,
            Authentication authentication) {

        Long idUsuario = idUsuario(authentication);
        return ResponseEntity.ok(notificacionService.resumen(idUsuario, idPerfilContexto));
    }

    @PatchMapping("/{idNotificacion}/leida")
    public ResponseEntity<NotificacionResponse> marcarLeida(
            @PathVariable Long idNotificacion,
            Authentication authentication) {

        Long idUsuario = idUsuario(authentication);
        return ResponseEntity.ok(notificacionService.marcarLeida(idUsuario, idNotificacion));
    }

    @PatchMapping("/leidas")
    public ResponseEntity<NotificacionMarcadoResponse> marcarTodasLeidas(
            @RequestParam(required = false) Long idPerfilContexto,
            Authentication authentication) {

        Long idUsuario = idUsuario(authentication);
        int actualizadas = notificacionService.marcarTodasLeidas(idUsuario, idPerfilContexto);
        return ResponseEntity.ok(new NotificacionMarcadoResponse(actualizadas));
    }

    private Long idUsuario(Authentication authentication) {
        return Long.parseLong((String) authentication.getPrincipal());
    }
}
