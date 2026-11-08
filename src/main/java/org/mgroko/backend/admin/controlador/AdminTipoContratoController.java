package org.mgroko.backend.admin.controlador;

import java.util.List;

import org.mgroko.backend.admin.dto.AdminTipoContratoCreateRequest;
import org.mgroko.backend.admin.dto.AdminTipoContratoEstadoRequest;
import org.mgroko.backend.admin.dto.AdminTipoContratoResponse;
import org.mgroko.backend.admin.dto.AdminTipoContratoResumenResponse;
import org.mgroko.backend.admin.dto.AdminTipoContratoUpdateRequest;
import org.mgroko.backend.admin.dto.ValidacionPlantillaResponse;
import org.mgroko.backend.admin.dto.ValidarPlantillaRequest;
import org.mgroko.backend.admin.servicio.AdminTipoContratoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * UC-76 — Gestionar tipos de contrato desde el panel de administración.
 */
@RestController
@RequestMapping("/admin/tipos-contrato")
public class AdminTipoContratoController {

    private final AdminTipoContratoService adminTipoContratoService;

    public AdminTipoContratoController(AdminTipoContratoService adminTipoContratoService) {
        this.adminTipoContratoService = adminTipoContratoService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VER_TIPOS_CONTRATO')")
    public ResponseEntity<List<AdminTipoContratoResumenResponse>> listar(
            @RequestParam(required = false) String nombre,
            @RequestParam(defaultValue = "true") boolean incluirInactivos) {
        return ResponseEntity.ok(
                adminTipoContratoService.listar(nombre, incluirInactivos));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VER_TIPOS_CONTRATO')")
    public ResponseEntity<AdminTipoContratoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(adminTipoContratoService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CREAR_TIPO_CONTRATO')")
    public ResponseEntity<AdminTipoContratoResponse> crear(
            @Valid @RequestBody AdminTipoContratoCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminTipoContratoService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MODIFICAR_TIPO_CONTRATO')")
    public ResponseEntity<AdminTipoContratoResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody AdminTipoContratoUpdateRequest request) {
        return ResponseEntity.ok(adminTipoContratoService.actualizar(id, request));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('MODIFICAR_TIPO_CONTRATO')")
    public ResponseEntity<AdminTipoContratoResponse> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody AdminTipoContratoEstadoRequest request) {
        return ResponseEntity.ok(adminTipoContratoService.cambiarEstado(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ELIMINAR_TIPO_CONTRATO')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        adminTipoContratoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/validar-plantilla")
    @PreAuthorize("hasAuthority('CREAR_TIPO_CONTRATO')")
    public ResponseEntity<ValidacionPlantillaResponse> validarPlantilla(
            @Valid @RequestBody ValidarPlantillaRequest request) {
        return ResponseEntity.ok(adminTipoContratoService.validarPlantilla(request));
    }
}
