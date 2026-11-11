package org.mgroko.backend.polaroids.mapper;

import org.mgroko.backend.modelo.PolaroidPerfil;
import org.mgroko.backend.polaroids.dto.PolaroidResponse;
import org.mgroko.backend.polaroids.dto.TipoPolaroidResponse;
import org.mgroko.backend.polaroids.servicio.PolaroidCatalogoService;

public final class PolaroidMapper {

    private PolaroidMapper() {
    }

    public static PolaroidResponse toResponse(
            PolaroidPerfil polaroid,
            PolaroidCatalogoService catalogoService) {
        TipoPolaroidResponse tipo = catalogoService.toResponse(polaroid.getTipoPolaroid());
        return new PolaroidResponse(
                polaroid.getIdPolaroid(),
                polaroid.getImagen().getIdImagen(),
                polaroid.getImagen().getUrl(),
                polaroid.getOrden(),
                polaroid.getFechaRegistro(),
                tipo);
    }
}
