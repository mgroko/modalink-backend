package org.mgroko.backend.ubicacion.controlador;

import java.util.List;

import org.mgroko.backend.ubicacion.catalogo.LocalidadCatalogo;
import org.mgroko.backend.ubicacion.catalogo.ProvinciaCatalogo;
import org.mgroko.backend.ubicacion.dto.LocalidadResponse;
import org.mgroko.backend.ubicacion.dto.ProvinciaCatalogoResponse;
import org.mgroko.backend.ubicacion.servicio.CatalogoGeograficoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ubicaciones")
public class UbicacionCatalogoController {

    private final CatalogoGeograficoService catalogoGeografico;

    public UbicacionCatalogoController(CatalogoGeograficoService catalogoGeografico) {
        this.catalogoGeografico = catalogoGeografico;
    }

    @GetMapping("/provincias")
    public ResponseEntity<List<ProvinciaCatalogoResponse>> listarProvincias() {
        return ResponseEntity.ok(catalogoGeografico.listarProvincias().stream()
                .map(this::toResponse)
                .toList());
    }

    @GetMapping("/localidades")
    public ResponseEntity<List<LocalidadResponse>> buscarLocalidades(
            @RequestParam(required = false) String provinciaId,
            @RequestParam(required = false) String nombre) {
        return ResponseEntity.ok(catalogoGeografico.buscarLocalidades(provinciaId, nombre).stream()
                .map(this::toResponse)
                .toList());
    }

    private ProvinciaCatalogoResponse toResponse(ProvinciaCatalogo provincia) {
        return new ProvinciaCatalogoResponse(provincia.idExterno(), provincia.nombre());
    }

    private LocalidadResponse toResponse(LocalidadCatalogo localidad) {
        return new LocalidadResponse(
                localidad.idExterno(),
                localidad.nombre(),
                localidad.idProvincia(),
                localidad.nombreProvincia(),
                localidad.latitud(),
                localidad.longitud());
    }
}