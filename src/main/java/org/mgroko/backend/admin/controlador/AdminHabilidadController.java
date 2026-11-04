package org.mgroko.backend.admin.controlador;

import java.util.List;

import org.mgroko.backend.admin.dto.AdminHabilidadRequest;
import org.mgroko.backend.admin.servicio.AdminHabilidadService;
import org.mgroko.backend.perfiles.dto.HabilidadResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * UC-56 — Gestionar habilidades del sistema.
 */
@RestController
@RequestMapping("/admin/habilidades")
public class AdminHabilidadController {

    private final AdminHabilidadService adminHabilidadService;

    public AdminHabilidadController(AdminHabilidadService adminHabilidadService) {
        this.adminHabilidadService = adminHabilidadService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VER_HABILIDADES')")
    public ResponseEntity<List<HabilidadResponse>> listar(
            @RequestParam(required = false) String nombre) {
        return ResponseEntity.ok(adminHabilidadService.listar(nombre));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VER_HABILIDADES')")
    public ResponseEntity<HabilidadResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(adminHabilidadService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CREAR_HABILIDAD')")
    public ResponseEntity<HabilidadResponse> crear(
            @Valid @RequestBody AdminHabilidadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminHabilidadService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MODIFICAR_HABILIDAD')")
    public ResponseEntity<HabilidadResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody AdminHabilidadRequest request) {
        return ResponseEntity.ok(adminHabilidadService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ELIMINAR_HABILIDAD')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        adminHabilidadService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
