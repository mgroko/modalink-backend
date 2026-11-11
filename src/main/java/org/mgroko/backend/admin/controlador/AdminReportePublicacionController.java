package org.mgroko.backend.admin.controlador;

import org.mgroko.backend.admin.dto.ActualizarEstadoReporteRequest;
import org.mgroko.backend.admin.dto.AdminReportePublicacionDetalleResponse;
import org.mgroko.backend.admin.dto.AdminReportePublicacionResumenResponse;
import org.mgroko.backend.admin.dto.BuscarReportesPublicacionFiltro;
import org.mgroko.backend.admin.servicio.AdminReportePublicacionService;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * UC-69: bandeja de revisión de reportes de publicación.
 *
 * <p>Solo accesible para usuarios con rol global Administrador. No requiere un
 * permiso global específico ni un perfil activo.</p>
 */
@RestController
@RequestMapping("/admin/reportes-publicacion")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminReportePublicacionController {

    private final AdminReportePublicacionService adminReportePublicacionService;

    public AdminReportePublicacionController(
            AdminReportePublicacionService adminReportePublicacionService) {
        this.adminReportePublicacionService = adminReportePublicacionService;
    }

    @GetMapping
    public ResponseEntity<PaginaResponse<AdminReportePublicacionResumenResponse>> listar(
            @ModelAttribute BuscarReportesPublicacionFiltro filtro,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean todos) {

        return ResponseEntity.ok(
                adminReportePublicacionService.buscar(filtro, page, size, todos));
    }

    @GetMapping("/{idReporte}")
    public ResponseEntity<AdminReportePublicacionDetalleResponse> detalle(
            @PathVariable Long idReporte) {

        return ResponseEntity.ok(
                adminReportePublicacionService.obtenerDetalle(idReporte));
    }

    @PatchMapping("/{idReporte}/estado")
    public ResponseEntity<AdminReportePublicacionDetalleResponse> actualizarEstado(
            @PathVariable Long idReporte,
            @Valid @RequestBody ActualizarEstadoReporteRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(adminReportePublicacionService.actualizarEstado(
                idReporte, idUsuario(authentication), request));
    }

    private Long idUsuario(Authentication authentication) {
        return Long.parseLong((String) authentication.getPrincipal());
    }
}
