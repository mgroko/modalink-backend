package org.mgroko.backend.actividades.controlador;

import org.mgroko.backend.actividades.dto.ActividadBusquedaResponse;
import org.mgroko.backend.actividades.dto.BuscarActividadesFiltro;
import org.mgroko.backend.actividades.servicio.BuscarActividadService;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ActividadController {

    private final BuscarActividadService buscarActividadService;

    public ActividadController(BuscarActividadService buscarActividadService) {
        this.buscarActividadService = buscarActividadService;
    }

    // UC-52: Buscar actividades dentro de la planificación de un proyecto
    @GetMapping("/proyectos/{idProyecto}/actividades/buscar")
    public ResponseEntity<PaginaResponse<ActividadBusquedaResponse>> buscar(
            @PathVariable Long idProyecto,
            @ModelAttribute BuscarActividadesFiltro filtro,
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

        return ResponseEntity.ok(
                buscarActividadService.buscar(idPerfilActivo, idProyecto, filtro, page, size, todos));
    }
}
