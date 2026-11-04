package org.mgroko.backend.perfiles.evento;

import java.util.List;

public record HabilidadModificadaEvent(
        Long idHabilidad,
        String nombreHabilidad,
        List<Long> idsPerfilesDestinatarios
) {
}
