package org.mgroko.backend.admin.controlador;

import java.time.LocalDate;

import org.mgroko.backend.admin.dto.ErrorProcesamientoHistoricoResponse;
import org.mgroko.backend.admin.dto.PreviewProcesamientoHistoricoResponse;
import org.mgroko.backend.admin.dto.ProcesamientoHistoricoResponse;
import org.mgroko.backend.admin.dto.ProcesarColaboracionesHistoricasRequest;
import org.mgroko.backend.admin.dto.PrevisualizarColaboracionesHistoricasRequest;
import org.mgroko.backend.admin.servicio.AdminColaboracionHistoricaService;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.enums.EstadoProcesamientoHistorico;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * Procesamiento histórico masivo de colaboraciones automáticas.
 */
@RestController
@RequestMapping("/admin/colaboraciones-automaticas/historicas")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminColaboracionHistoricaController {

    private final AdminColaboracionHistoricaService service;

    public AdminColaboracionHistoricaController(
            AdminColaboracionHistoricaService service) {
        this.service = service;
    }

    @PostMapping("/previsualizar")
    public ResponseEntity<PreviewProcesamientoHistoricoResponse> previsualizar(
            @Valid @RequestBody PrevisualizarColaboracionesHistoricasRequest request) {
        return ResponseEntity.ok(service.previsualizar(request));
    }

    @PostMapping("/procesar")
    public ResponseEntity<ProcesamientoHistoricoResponse> procesar(
            @Valid @RequestBody ProcesarColaboracionesHistoricasRequest request,
            Authentication authentication) {

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                service.procesar(request, idUsuario(authentication)));
    }

    @GetMapping
    public ResponseEntity<PaginaResponse<ProcesamientoHistoricoResponse>> listar(
            @RequestParam(required = false) EstadoProcesamientoHistorico estado,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(
                service.listar(estado, fechaDesde, fechaHasta, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProcesamientoHistoricoResponse> obtener(
            @PathVariable Long id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @GetMapping("/{id}/errores")
    public ResponseEntity<PaginaResponse<ErrorProcesamientoHistoricoResponse>> listarErrores(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(service.listarErrores(id, page, size));
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<ProcesamientoHistoricoResponse> cancelar(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(
                service.cancelar(id, idUsuario(authentication)));
    }

    @PostMapping("/{id}/reanudar")
    public ResponseEntity<ProcesamientoHistoricoResponse> reanudar(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                service.reanudar(id, idUsuario(authentication)));
    }

    private Long idUsuario(Authentication authentication) {
        return Long.parseLong((String) authentication.getPrincipal());
    }
}
