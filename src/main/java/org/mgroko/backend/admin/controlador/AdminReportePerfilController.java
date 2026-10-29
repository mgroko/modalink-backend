package org.mgroko.backend.admin.controlador;

import org.mgroko.backend.admin.dto.ActualizarEstadoReporteRequest;
import org.mgroko.backend.admin.dto.AdminReportePerfilDetalleResponse;
import org.mgroko.backend.admin.dto.AdminReportePerfilResumenResponse;
import org.mgroko.backend.admin.dto.BuscarReportesPerfilFiltro;
import org.mgroko.backend.admin.servicio.AdminReportePerfilService;
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
 * UC-15: bandeja de revisión de reportes de perfil.
 *
 * <p>Solo accesible para usuarios con rol global Administrador. No requiere un
 * permiso global específico ni un perfil activo.</p>
 */
@RestController
@RequestMapping("/admin/reportes-perfil")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminReportePerfilController {

    private final AdminReportePerfilService adminReportePerfilService;

    public AdminReportePerfilController(AdminReportePerfilService adminReportePerfilService) {
        this.adminReportePerfilService = adminReportePerfilService;
    }

    @GetMapping
    public ResponseEntity<PaginaResponse<AdminReportePerfilResumenResponse>> listar(
            @ModelAttribute BuscarReportesPerfilFiltro filtro,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean todos) {

        return ResponseEntity.ok(
                adminReportePerfilService.buscar(filtro, page, size, todos));
    }

    @GetMapping("/{idReporte}")
    public ResponseEntity<AdminReportePerfilDetalleResponse> detalle(
            @PathVariable Long idReporte) {

        return ResponseEntity.ok(adminReportePerfilService.obtenerDetalle(idReporte));
    }

    @PatchMapping("/{idReporte}/estado")
    public ResponseEntity<AdminReportePerfilDetalleResponse> actualizarEstado(
            @PathVariable Long idReporte,
            @Valid @RequestBody ActualizarEstadoReporteRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(adminReportePerfilService.actualizarEstado(
                idReporte, idUsuario(authentication), request));
    }

    private Long idUsuario(Authentication authentication) {
        return Long.parseLong((String) authentication.getPrincipal());
    }
}
