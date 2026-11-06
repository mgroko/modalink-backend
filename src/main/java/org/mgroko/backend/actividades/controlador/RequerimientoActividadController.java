package org.mgroko.backend.actividades.controlador;

import org.mgroko.backend.actividades.dto.BuscarRequerimientosActividadFiltro;
import org.mgroko.backend.actividades.dto.RequerimientoActividadBusquedaResponse;
import org.mgroko.backend.actividades.servicio.BuscarRequerimientoActividadService;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * UC-73: búsqueda de requerimientos de actividad dentro de un proyecto.
 */
@RestController
@PreAuthorize("hasRole('USUARIO')")
public class RequerimientoActividadController {

    private final BuscarRequerimientoActividadService buscarRequerimientoActividadService;

    public RequerimientoActividadController(
            BuscarRequerimientoActividadService buscarRequerimientoActividadService) {
        this.buscarRequerimientoActividadService = buscarRequerimientoActividadService;
    }

    @GetMapping("/proyectos/{idProyecto}/requerimientos-actividad/buscar")
    public ResponseEntity<PaginaResponse<RequerimientoActividadBusquedaResponse>> buscar(
            @PathVariable Long idProyecto,
            @ModelAttribute BuscarRequerimientosActividadFiltro filtro,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean todos,
            Authentication authentication) {

        Long idPerfilActivo = null;
        if (authentication.getDetails() instanceof ContextoAutenticacion contexto) {
            idPerfilActivo = contexto.idPerfilActivo();
        }

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }

        return ResponseEntity.ok(buscarRequerimientoActividadService.buscar(
                idPerfilActivo, idProyecto, filtro, page, size, todos));
    }
}
