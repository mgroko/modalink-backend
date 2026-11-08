package org.mgroko.backend.proyectos.controlador;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.contratos.dto.HistorialContratoEventoResponse;
import org.mgroko.backend.contratos.servicio.ListarHistorialContratosProyectoService;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.proyectos.dto.HistorialProyectoEventoResponse;
import org.mgroko.backend.proyectos.servicio.ListarHistorialProyectoService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Historial del proyecto.
 *
 * <p>La sección de membresías terminadas es exclusiva del Director activo. La
 * sección de contratos también está disponible para miembros activos: el
 * Director ve todo el proyecto y cada miembro ve sus propios contratos.</p>
 */
@RestController
@PreAuthorize("hasRole('USUARIO')")
public class HistorialProyectoController {

    private final ListarHistorialProyectoService listarHistorialProyectoService;
    private final ListarHistorialContratosProyectoService listarHistorialContratosService;

    public HistorialProyectoController(
            ListarHistorialProyectoService listarHistorialProyectoService,
            ListarHistorialContratosProyectoService listarHistorialContratosService) {
        this.listarHistorialProyectoService = listarHistorialProyectoService;
        this.listarHistorialContratosService = listarHistorialContratosService;
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

    /**
     * Historial de contratos del proyecto. El Director ve todos los eventos;
     * un miembro ve solo los de sus propios contratos. Incluye contratos
     * eliminados y proyectos finalizados porque es una vista de solo lectura.
     */
    @GetMapping("/proyectos/{idProyecto}/historial/contratos")
    public ResponseEntity<PaginaResponse<HistorialContratoEventoResponse>> listarContratos(
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

        return ResponseEntity.ok(listarHistorialContratosService.listar(
                idUsuario,
                idPerfilActivo,
                idProyecto,
                tipo,
                nombre,
                page,
                size));
    }
}
