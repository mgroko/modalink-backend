package org.mgroko.backend.publicaciones.controlador;

import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.publicaciones.dto.EnviarSolicitudColaboracionRequest;
import org.mgroko.backend.publicaciones.dto.SolicitudColaboracionResponse;
import org.mgroko.backend.publicaciones.servicio.EnviarSolicitudColaboracionService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * UC-61 - Enviar solicitud de colaboración.
 */
@RestController
@PreAuthorize("hasRole('USUARIO')")
public class SolicitudColaboracionController {

    private final EnviarSolicitudColaboracionService enviarSolicitudColaboracionService;

    public SolicitudColaboracionController(
            EnviarSolicitudColaboracionService enviarSolicitudColaboracionService) {
        this.enviarSolicitudColaboracionService = enviarSolicitudColaboracionService;
    }

    @PostMapping("/publicaciones/{idPublicacion}/solicitudes-colaboracion")
    public ResponseEntity<SolicitudColaboracionResponse> enviar(
            @PathVariable Long idPublicacion,
            @Valid @RequestBody EnviarSolicitudColaboracionRequest request,
            Authentication authentication) {

        SolicitudColaboracionResponse response = enviarSolicitudColaboracionService.enviar(
                idUsuario(authentication),
                idPerfilActivoRequerido(authentication),
                idPublicacion,
                request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private Long idUsuario(Authentication authentication) {
        return Long.parseLong((String) authentication.getPrincipal());
    }

    private Long idPerfilActivoRequerido(Authentication authentication) {
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
