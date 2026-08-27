package org.mgroko.backend.usuario.controlador;

import org.mgroko.backend.usuario.dto.ReactivarCuentaResponse;
import org.mgroko.backend.usuario.servicio.ReactivarCuentaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usuario")
public class ReactivarCuentaController {

    private final ReactivarCuentaService reactivarCuentaService;

    public ReactivarCuentaController(ReactivarCuentaService reactivarCuentaService) {
        this.reactivarCuentaService = reactivarCuentaService;
    }

    @PostMapping("/reactivar-cuenta")
    public ResponseEntity<ReactivarCuentaResponse> reactivarCuenta(Authentication authentication) {
        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());
        ReactivarCuentaResponse response = reactivarCuentaService.reactivarCuenta(idUsuario);
        return ResponseEntity.ok(response);
    }
}
