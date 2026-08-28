package org.mgroko.backend.perfiles.controlador;

import java.util.List;

import org.mgroko.backend.perfiles.dto.ProfesionResponse;
import org.mgroko.backend.perfiles.servicio.ProfesionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/profesiones")
public class ProfesionController {

    private final ProfesionService profesionService;

    public ProfesionController(ProfesionService profesionService) {
        this.profesionService = profesionService;
    }

    @GetMapping
    public ResponseEntity<List<ProfesionResponse>> buscar(@RequestParam(required = false) String nombre) {
        return ResponseEntity.ok(profesionService.buscar(nombre));
    }
}