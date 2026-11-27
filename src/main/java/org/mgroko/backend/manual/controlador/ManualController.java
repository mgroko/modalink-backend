package org.mgroko.backend.manual.controlador;

import java.util.List;

import org.mgroko.backend.manual.dto.ManualCapituloResponse;
import org.mgroko.backend.manual.dto.ManualError;
import org.mgroko.backend.manual.dto.ManualIndiceResponse;
import org.mgroko.backend.manual.dto.ManualResultado;
import org.mgroko.backend.manual.servicio.ManualService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Manual de usuario in-app. El índice y los capítulos se filtran según el rol
 * autenticado: USUARIO ve el manual de usuario y ADMINISTRADOR ve el manual
 * administrativo. Ambos roles pueden consultar el Anexo A.
 */
@RestController
@RequestMapping("/ayuda")
public class ManualController {

    private final ManualService manualService;

    public ManualController(ManualService manualService) {
        this.manualService = manualService;
    }

    @GetMapping("/indice")
    public ResponseEntity<ManualIndiceResponse> indice(Authentication authentication) {
        return ResponseEntity.ok(manualService.indice(esAdmin(authentication)));
    }

    @GetMapping("/capitulos/{id}")
    public ResponseEntity<ManualCapituloResponse> capitulo(
            @PathVariable String id,
            Authentication authentication) {
        return ResponseEntity.ok(manualService.capitulo(id, esAdmin(authentication)));
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<ManualResultado>> buscar(
            @RequestParam(name = "q", defaultValue = "") String consulta,
            Authentication authentication) {
        return ResponseEntity.ok(manualService.buscar(consulta, esAdmin(authentication)));
    }

    @GetMapping("/errores")
    public ResponseEntity<List<ManualError>> errores(Authentication authentication) {
        return ResponseEntity.ok(manualService.errores(esAdmin(authentication)));
    }

    private boolean esAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(autoridad -> "ROLE_ADMINISTRADOR".equals(autoridad.getAuthority()));
    }
}
