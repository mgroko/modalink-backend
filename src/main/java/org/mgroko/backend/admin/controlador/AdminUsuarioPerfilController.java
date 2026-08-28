package org.mgroko.backend.admin.controlador;

import java.util.List;

import org.mgroko.backend.admin.dto.AdminPerfilResponse;
import org.mgroko.backend.perfiles.servicio.UsuarioPerfilService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/usuarios/{id}/perfiles")
public class AdminUsuarioPerfilController {

    private final UsuarioPerfilService usuarioPerfilService;

    public AdminUsuarioPerfilController(UsuarioPerfilService usuarioPerfilService) {
        this.usuarioPerfilService = usuarioPerfilService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VER_USUARIOS')")
    public ResponseEntity<List<AdminPerfilResponse>> listarPerfiles(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioPerfilService.listarPerfiles(id));
    }
}
