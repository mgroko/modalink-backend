package org.mgroko.backend.admin.controlador;

import java.util.List;

import org.mgroko.backend.admin.dto.AdminUnidadMedidaRequest;
import org.mgroko.backend.admin.dto.UnidadMedidaResponse;
import org.mgroko.backend.admin.servicio.AdminUnidadMedidaService;
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

@RestController
@RequestMapping("/admin/unidades-medida")
public class AdminUnidadMedidaController {

    private final AdminUnidadMedidaService adminUnidadMedidaService;

    public AdminUnidadMedidaController(AdminUnidadMedidaService adminUnidadMedidaService) {
        this.adminUnidadMedidaService = adminUnidadMedidaService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VER_CARACTERISTICAS')")
    public ResponseEntity<List<UnidadMedidaResponse>> listar(
            @RequestParam(required = false) String tipoDato) {
        return ResponseEntity.ok(adminUnidadMedidaService.listar(tipoDato));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VER_CARACTERISTICAS')")
    public ResponseEntity<UnidadMedidaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(adminUnidadMedidaService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CREAR_CARACTERISTICA')")
    public ResponseEntity<UnidadMedidaResponse> crear(
            @Valid @RequestBody AdminUnidadMedidaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminUnidadMedidaService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MODIFICAR_CARACTERISTICA')")
    public ResponseEntity<UnidadMedidaResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody AdminUnidadMedidaRequest request) {
        return ResponseEntity.ok(adminUnidadMedidaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ELIMINAR_CARACTERISTICA')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        adminUnidadMedidaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
