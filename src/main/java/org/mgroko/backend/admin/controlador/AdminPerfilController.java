package org.mgroko.backend.admin.controlador;

import org.mgroko.backend.admin.dto.AdminPerfilDetalleResponse;
import org.mgroko.backend.admin.dto.AdminPerfilResumenResponse;
import org.mgroko.backend.admin.dto.BuscarPerfilesAdminFiltro;
import org.mgroko.backend.admin.servicio.AdminPerfilService;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Consulta administrativa de perfiles. Solo lectura.
 */
@RestController
@RequestMapping("/admin/perfiles")
@PreAuthorize("hasAuthority('VER_PERFILES')")
@Validated
public class AdminPerfilController {

    private final AdminPerfilService adminPerfilService;

    public AdminPerfilController(AdminPerfilService adminPerfilService) {
        this.adminPerfilService = adminPerfilService;
    }

    @GetMapping
    public ResponseEntity<PaginaResponse<AdminPerfilResumenResponse>> listar(
            @ModelAttribute BuscarPerfilesAdminFiltro filtro,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "false") boolean todos) {

        return ResponseEntity.ok(adminPerfilService.buscar(filtro, page, size, todos));
    }

    @GetMapping("/{idPerfil}")
    public ResponseEntity<AdminPerfilDetalleResponse> detalle(
            @PathVariable Long idPerfil) {

        return ResponseEntity.ok(adminPerfilService.obtenerDetalle(idPerfil));
    }
}
