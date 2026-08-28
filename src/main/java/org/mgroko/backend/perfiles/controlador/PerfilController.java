package org.mgroko.backend.perfiles.controlador;

import java.util.List;

import org.mgroko.backend.perfiles.dto.CrearPerfilRequest;
import org.mgroko.backend.perfiles.dto.PerfilResponse;
import org.mgroko.backend.perfiles.servicio.CrearPerfilService;
import org.mgroko.backend.perfiles.servicio.UsuarioPerfilService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
public class PerfilController {

    private final CrearPerfilService crearPerfilService;
    private final UsuarioPerfilService usuarioPerfilService;

    public PerfilController(CrearPerfilService crearPerfilService, UsuarioPerfilService usuarioPerfilService) {
        this.crearPerfilService = crearPerfilService;
        this.usuarioPerfilService = usuarioPerfilService;
    }

    @PostMapping("/perfiles")
    public ResponseEntity<PerfilResponse> crear(
            @Valid @RequestBody CrearPerfilRequest request,
            Authentication authentication) {
        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());
        PerfilResponse response = crearPerfilService.crear(idUsuario, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/usuarios/me/perfiles")
    public ResponseEntity<List<PerfilResponse>> listarPerfilesPropios(Authentication authentication) {
        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());
        List<PerfilResponse> response = usuarioPerfilService.listarPerfilesPropios(idUsuario);
        return ResponseEntity.ok(response);
    }
}