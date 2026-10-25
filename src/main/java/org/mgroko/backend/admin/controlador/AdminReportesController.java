package org.mgroko.backend.admin.controlador;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/reportes")
@PreAuthorize("hasAuthority('VER_USUARIOS')")
public class AdminReportesController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> listar() {
        return noImplementado();
    }

    @GetMapping("/{reporteTipo}")
    public ResponseEntity<Map<String, Object>> obtener(
            @org.springframework.web.bind.annotation.PathVariable String reporteTipo) {
        return noImplementado();
    }

    private ResponseEntity<Map<String, Object>> noImplementado() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of(
                "message", "Los reportes administrativos aún no están implementados.",
                "httpStatus", HttpStatus.NOT_IMPLEMENTED.value()));
    }
}