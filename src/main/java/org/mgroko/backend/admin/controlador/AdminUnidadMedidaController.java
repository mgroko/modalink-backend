package org.mgroko.backend.admin.controlador;

import java.util.List;

import org.mgroko.backend.admin.dto.UnidadMedidaResponse;
import org.mgroko.backend.modelo.UnidadMedida;
import org.mgroko.backend.repositorio.UnidadMedidaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/unidades-medida")
public class AdminUnidadMedidaController {

    private final UnidadMedidaRepository unidadMedidaRepository;

    public AdminUnidadMedidaController(UnidadMedidaRepository unidadMedidaRepository) {
        this.unidadMedidaRepository = unidadMedidaRepository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VER_CARACTERISTICAS')")
    public ResponseEntity<List<UnidadMedidaResponse>> listar(
            @RequestParam(required = false) String tipoDato) {
        List<UnidadMedida> unidades = unidadMedidaRepository.findAll();

        if (tipoDato != null && !tipoDato.isBlank()) {
            String tipoFiltro = tipoDato.trim().toUpperCase();
            unidades = unidades.stream()
                    .filter(u -> u.getTipoDatoPermitido() != null && u.getTipoDatoPermitido().equalsIgnoreCase(tipoFiltro))
                    .toList();
        }

        List<UnidadMedidaResponse> response = unidades.stream()
                .map(u -> new UnidadMedidaResponse(
                        u.getIdUnidad(),
                        u.getNombre(),
                        u.getSimbolo(),
                        u.getTipoDatoPermitido()))
                .toList();

        return ResponseEntity.ok(response);
    }
}
