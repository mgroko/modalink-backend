package org.mgroko.backend.perfiles.controlador;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.perfiles.dto.HistorialProyectoPerfilResponse;
import org.mgroko.backend.perfiles.servicio.ListarHistorialProyectosPerfilService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('USUARIO')")
public class PerfilProyectoController {

    private final ListarHistorialProyectosPerfilService listarHistorialProyectosPerfilService;

    public PerfilProyectoController(
            ListarHistorialProyectosPerfilService listarHistorialProyectosPerfilService) {
        this.listarHistorialProyectosPerfilService = listarHistorialProyectosPerfilService;
    }

    @GetMapping("/perfiles/{idPerfil}/proyectos/historial")
    public ResponseEntity<PaginaResponse<HistorialProyectoPerfilResponse>> listarHistorial(
            @PathVariable Long idPerfil,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());
        Long idPerfilActivo = null;
        if (authentication.getDetails() instanceof ContextoAutenticacion contexto) {
            idPerfilActivo = contexto.idPerfilActivo();
        }

        return ResponseEntity.ok(listarHistorialProyectosPerfilService.listar(
                idPerfil, idUsuario, idPerfilActivo, page, size));
    }
}
