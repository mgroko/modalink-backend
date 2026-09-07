package org.mgroko.backend.admin.controlador;

import java.util.List;

import org.mgroko.backend.admin.dto.AdminUsuarioResponse;
import org.mgroko.backend.admin.dto.DeshabilitarUsuarioRequest;
import org.mgroko.backend.admin.servicio.AdminUsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/usuarios")
public class AdminUsuarioController {

    private final AdminUsuarioService adminUsuarioService;

    public AdminUsuarioController(AdminUsuarioService adminUsuarioService) {
        this.adminUsuarioService = adminUsuarioService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VER_USUARIOS')")
    public ResponseEntity<List<AdminUsuarioResponse>> listar() {
        return ResponseEntity.ok(adminUsuarioService.listar());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VER_USUARIOS')")
    public ResponseEntity<AdminUsuarioResponse> detalle(@PathVariable Long id) {
        return ResponseEntity.ok(adminUsuarioService.obtenerDetalle(id));
    }

    @PatchMapping("/{id}/habilitar")
    @PreAuthorize("hasAuthority('HABILITAR_USUARIO')")
    public ResponseEntity<AdminUsuarioResponse> habilitar(@PathVariable Long id) {
        return ResponseEntity.ok(adminUsuarioService.habilitar(id));
    }

    @PatchMapping("/{id}/deshabilitar")
    @PreAuthorize("hasAuthority('DESHABILITAR_USUARIO')")
    public ResponseEntity<AdminUsuarioResponse> deshabilitar(
            @PathVariable Long id,
            @Valid @RequestBody DeshabilitarUsuarioRequest request,
            Authentication authentication) {
        Long idSolicitante = Long.parseLong((String) authentication.getPrincipal());
        return ResponseEntity.ok(adminUsuarioService.deshabilitar(id, idSolicitante, request));
    }
}