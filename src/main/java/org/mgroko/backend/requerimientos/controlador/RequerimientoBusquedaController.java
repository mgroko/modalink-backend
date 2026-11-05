package org.mgroko.backend.requerimientos.controlador;

import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.requerimientos.dto.BuscarRequerimientosGlobalFiltro;
import org.mgroko.backend.requerimientos.dto.RequerimientoOportunidadResponse;
import org.mgroko.backend.requerimientos.servicio.BuscarRequerimientosActividadGlobalService;
import org.mgroko.backend.requerimientos.servicio.BuscarRequerimientosGeneralesService;
import org.mgroko.backend.security.ContextoAutenticacion;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Buscador global de requerimientos abiertos para postularse.
 *
 * <p>UC-72 expone los requerimientos generales y UC-73 los de actividad. Las
 * pestañas del buscador consumen un endpoint cada una para mantener paginación
 * y orden independientes.</p>
 */
@RestController
@PreAuthorize("hasRole('USUARIO')")
public class RequerimientoBusquedaController {

    private final BuscarRequerimientosGeneralesService buscarRequerimientosGeneralesService;
    private final BuscarRequerimientosActividadGlobalService buscarRequerimientosActividadService;

    public RequerimientoBusquedaController(
            BuscarRequerimientosGeneralesService buscarRequerimientosGeneralesService,
            BuscarRequerimientosActividadGlobalService buscarRequerimientosActividadService) {
        this.buscarRequerimientosGeneralesService = buscarRequerimientosGeneralesService;
        this.buscarRequerimientosActividadService = buscarRequerimientosActividadService;
    }

    @GetMapping("/requerimientos-generales/buscar")
    public ResponseEntity<PaginaResponse<RequerimientoOportunidadResponse>> buscarGenerales(
            @ModelAttribute BuscarRequerimientosGlobalFiltro filtro,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean todos,
            Authentication authentication) {

        return ResponseEntity.ok(buscarRequerimientosGeneralesService.buscar(
                idPerfilActivo(authentication), filtro, page, size, todos));
    }

    @GetMapping("/requerimientos-actividad/buscar")
    public ResponseEntity<PaginaResponse<RequerimientoOportunidadResponse>> buscarActividades(
            @ModelAttribute BuscarRequerimientosGlobalFiltro filtro,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean todos,
            Authentication authentication) {

        return ResponseEntity.ok(buscarRequerimientosActividadService.buscar(
                idPerfilActivo(authentication), filtro, page, size, todos));
    }

    private Long idPerfilActivo(Authentication authentication) {
        Long idPerfilActivo = null;
        if (authentication != null
                && authentication.getDetails() instanceof ContextoAutenticacion contexto) {
            idPerfilActivo = contexto.idPerfilActivo();
        }

        if (idPerfilActivo == null) {
            throw new PerfilActivoNoSeleccionadoException();
        }
        return idPerfilActivo;
    }
}
