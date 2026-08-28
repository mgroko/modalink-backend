package org.mgroko.backend.admin.controlador;

import java.util.List;

import org.mgroko.backend.admin.dto.AdminCaracteristicaTecnicaRequest;
import org.mgroko.backend.admin.dto.AdminValorCaracteristicaRequest;
import org.mgroko.backend.admin.servicio.AdminCaracteristicaTecnicaService;
import org.mgroko.backend.perfiles.dto.CaracteristicaTecnicaResponse;
import org.mgroko.backend.perfiles.dto.ValorCaracteristicaResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/caracteristicas-tecnicas")
public class AdminCaracteristicaTecnicaController {

    private final AdminCaracteristicaTecnicaService adminCaracteristicaTecnicaService;

    public AdminCaracteristicaTecnicaController(
            AdminCaracteristicaTecnicaService adminCaracteristicaTecnicaService) {
        this.adminCaracteristicaTecnicaService = adminCaracteristicaTecnicaService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VER_CARACTERISTICAS')")
    public ResponseEntity<List<CaracteristicaTecnicaResponse>> listar() {
        return ResponseEntity.ok(adminCaracteristicaTecnicaService.listar());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CREAR_CARACTERISTICA')")
    public ResponseEntity<CaracteristicaTecnicaResponse> crear(
            @Valid @RequestBody AdminCaracteristicaTecnicaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminCaracteristicaTecnicaService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MODIFICAR_CARACTERISTICA')")
    public ResponseEntity<CaracteristicaTecnicaResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody AdminCaracteristicaTecnicaRequest request) {
        return ResponseEntity.ok(adminCaracteristicaTecnicaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ELIMINAR_CARACTERISTICA')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        adminCaracteristicaTecnicaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/valores")
    @PreAuthorize("hasAuthority('CREAR_CARACTERISTICA')")
    public ResponseEntity<ValorCaracteristicaResponse> agregarValor(
            @PathVariable Long id,
            @Valid @RequestBody AdminValorCaracteristicaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminCaracteristicaTecnicaService.agregarValor(id, request));
    }

    @PutMapping("/valores/{idValor}")
    @PreAuthorize("hasAuthority('MODIFICAR_CARACTERISTICA')")
    public ResponseEntity<ValorCaracteristicaResponse> actualizarValor(
            @PathVariable Long idValor,
            @Valid @RequestBody AdminValorCaracteristicaRequest request) {
        return ResponseEntity.ok(adminCaracteristicaTecnicaService.actualizarValor(idValor, request));
    }

    @DeleteMapping("/valores/{idValor}")
    @PreAuthorize("hasAuthority('ELIMINAR_CARACTERISTICA')")
    public ResponseEntity<Void> eliminarValor(@PathVariable Long idValor) {
        adminCaracteristicaTecnicaService.eliminarValor(idValor);
        return ResponseEntity.noContent().build();
    }
}