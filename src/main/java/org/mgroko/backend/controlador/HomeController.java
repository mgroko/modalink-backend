package org.mgroko.backend.controlador;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {
@GetMapping("/health")
public Map<String, String> health() {
    return Map.of("status", "ok");
}

}
