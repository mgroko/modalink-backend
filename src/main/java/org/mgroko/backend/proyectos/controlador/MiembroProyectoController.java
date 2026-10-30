package org.mgroko.backend.proyectos.controlador;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.proyectos.dto.MiembroProyectoResponse;
import org.mgroko.backend.proyectos.servicio.ListarMiembrosProyectoService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * UC-70: miembros activos del proyecto para seleccionar el destinatario de una
 * asignación.
 */
@RestController
public class MiembroProyectoController {

    private final ListarMiembrosProyectoService listarMiembrosProyectoService;

    public MiembroProyectoController(
            ListarMiembrosProyectoService listarMiembrosProyectoService) {
        this.listarMiembrosProyectoService = listarMiembrosProyectoService;
    }

    @GetMapping("/proyectos/{idProyecto}/miembros")
    public ResponseEntity<PaginaResponse<MiembroProyectoResponse>> listar(
            @PathVariable Long idProyecto,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) Long idProfesion,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean todos,
            Authentication authentication) {

        Long idUsuario = Long.parseLong((String) authentication.getPrincipal());

        Long idPerfilActivo = null;
        if (authentication.getDetails() instanceof ContextoAutenticacion contexto) {
            idPerfilActivo = contexto.idPerfilActivo();
        }

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        return ResponseEntity.ok(listarMiembrosProyectoService.listar(
                idUsuario,
                idPerfilActivo,
                idProyecto,
                nombre,
                rol,
                idProfesion,
                page,
                size,
                todos));
    }
}
