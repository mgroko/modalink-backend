package org.mgroko.backend.proyectos.controlador;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.proyectos.dto.HistorialProyectoEventoResponse;
import org.mgroko.backend.proyectos.servicio.ListarHistorialProyectoService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Historial de membresías terminadas del proyecto.
 *
 * <p>La pestaña Historial del frontend es exclusiva del Director activo. El
 * backend valida esa condición aunque la vista no se muestre.</p>
 */
@RestController
public class HistorialProyectoController {

    private final ListarHistorialProyectoService listarHistorialProyectoService;

    public HistorialProyectoController(
            ListarHistorialProyectoService listarHistorialProyectoService) {
        this.listarHistorialProyectoService = listarHistorialProyectoService;
    }

    @GetMapping("/proyectos/{idProyecto}/historial")
    public ResponseEntity<PaginaResponse<HistorialProyectoEventoResponse>> listar(
            @PathVariable Long idProyecto,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String nombre,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());

        Long idPerfilActivo = null;
        if (authentication.getDetails() instanceof ContextoAutenticacion contexto) {
            idPerfilActivo = contexto.idPerfilActivo();
        }

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        return ResponseEntity.ok(listarHistorialProyectoService.listar(
                idUsuario,
                idPerfilActivo,
                idProyecto,
                tipo,
                nombre,
                page,
                size));
    }
}
