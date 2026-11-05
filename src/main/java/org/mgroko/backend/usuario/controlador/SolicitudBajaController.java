package org.mgroko.backend.usuario.controlador;

import org.mgroko.backend.usuario.dto.SolicitudBajaResponse;
import org.mgroko.backend.usuario.servicio.SolicitudBajaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('USUARIO')")
@RequestMapping("/usuario")
public class SolicitudBajaController {

    private final SolicitudBajaService solicitudBajaService;

    public SolicitudBajaController(SolicitudBajaService solicitudBajaService) {
        this.solicitudBajaService = solicitudBajaService;
    }

    @PostMapping("/solicitar-baja")
    public ResponseEntity<SolicitudBajaResponse> solicitarBaja(Authentication authentication) {
        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());
        SolicitudBajaResponse response = solicitudBajaService.solicitarBaja(idUsuario);
        return ResponseEntity.ok(response);
    }
}
