package org.mgroko.backend.normas.controlador;

import java.util.List;

import org.mgroko.backend.normas.dto.NormasConvivenciaResponse;
import org.mgroko.backend.normas.dto.NormasConvivenciaVersionResumen;
import org.mgroko.backend.normas.servicio.NormasConvivenciaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contenido público de las normas de convivencia. No requiere sesión: la
 * página se enlaza desde el login, el registro y los avisos de incorporación
 * a proyectos.
 */
@RestController
@RequestMapping("/normas-convivencia")
public class NormasConvivenciaController {

    private final NormasConvivenciaService normasConvivenciaService;

    public NormasConvivenciaController(NormasConvivenciaService normasConvivenciaService) {
        this.normasConvivenciaService = normasConvivenciaService;
    }

    @GetMapping("/vigente")
    public ResponseEntity<NormasConvivenciaResponse> vigente() {
        return ResponseEntity.ok(normasConvivenciaService.vigente());
    }

    @GetMapping("/versiones")
    public ResponseEntity<List<NormasConvivenciaVersionResumen>> versiones() {
        return ResponseEntity.ok(normasConvivenciaService.versiones());
    }

    @GetMapping("/versiones/{version}")
    public ResponseEntity<NormasConvivenciaResponse> version(@PathVariable String version) {
        return ResponseEntity.ok(normasConvivenciaService.version(version));
    }
}
