package org.mgroko.backend.perfiles.controlador;

import java.util.List;

import org.mgroko.backend.perfiles.dto.HabilidadResponse;
import org.mgroko.backend.perfiles.servicio.HabilidadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * UC-48 — Buscar habilidades.
 */
@RestController
@RequestMapping("/habilidades")
public class HabilidadController {

    private final HabilidadService habilidadService;

    public HabilidadController(HabilidadService habilidadService) {
        this.habilidadService = habilidadService;
    }

    @GetMapping
    public ResponseEntity<List<HabilidadResponse>> buscar(
            @RequestParam(required = false) String nombre) {
        return ResponseEntity.ok(habilidadService.buscar(nombre));
    }
}
