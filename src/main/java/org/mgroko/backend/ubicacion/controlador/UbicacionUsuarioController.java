package org.mgroko.backend.ubicacion.controlador;

import org.mgroko.backend.ubicacion.dto.UbicacionRequest;
import org.mgroko.backend.ubicacion.dto.UbicacionResponse;
import org.mgroko.backend.ubicacion.servicio.UbicacionUsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/usuario/ubicacion")
public class UbicacionUsuarioController {

    private final UbicacionUsuarioService ubicacionUsuarioService;

    public UbicacionUsuarioController(UbicacionUsuarioService ubicacionUsuarioService) {
        this.ubicacionUsuarioService = ubicacionUsuarioService;
    }

    @GetMapping
    public ResponseEntity<UbicacionResponse> obtener(Authentication authentication) {
        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());
        return ubicacionUsuarioService.obtener(idUsuario)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok().build());
    }

    @PutMapping
    public ResponseEntity<UbicacionResponse> asignar(
            @Valid @RequestBody UbicacionRequest request,
            Authentication authentication) {
        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());
        UbicacionResponse response = ubicacionUsuarioService.asignar(idUsuario, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<Void> quitar(Authentication authentication) {
        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());
        ubicacionUsuarioService.quitar(idUsuario);
        return ResponseEntity.noContent().build();
    }
}