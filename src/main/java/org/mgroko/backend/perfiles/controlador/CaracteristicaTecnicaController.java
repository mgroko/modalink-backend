package org.mgroko.backend.perfiles.controlador;

import java.util.List;

import org.mgroko.backend.perfiles.dto.CaracteristicaTecnicaResponse;
import org.mgroko.backend.perfiles.servicio.CaracteristicaTecnicaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CaracteristicaTecnicaController {

    private final CaracteristicaTecnicaService caracteristicaTecnicaService;

    public CaracteristicaTecnicaController(CaracteristicaTecnicaService caracteristicaTecnicaService) {
        this.caracteristicaTecnicaService = caracteristicaTecnicaService;
    }

    @GetMapping("/profesiones/{id}/caracteristicas-tecnicas")
    public ResponseEntity<List<CaracteristicaTecnicaResponse>> buscar(
            @PathVariable Long id,
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) String unidad) {
        return ResponseEntity.ok(caracteristicaTecnicaService.buscar(id, codigo, unidad));
    }
}