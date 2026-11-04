package org.mgroko.backend.perfiles.evento;

import java.util.List;

public record HabilidadEliminadaEvent(
        Long idHabilidad,
        String nombreHabilidad,
        List<Long> idsPerfilesDestinatarios
) {
}
