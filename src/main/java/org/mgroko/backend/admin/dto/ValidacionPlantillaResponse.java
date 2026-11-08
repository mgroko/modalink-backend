package org.mgroko.backend.admin.dto;

import java.util.List;

public record ValidacionPlantillaResponse(
        boolean valido,
        int longitud,
        List<String> placeholdersDesconocidos,
        List<String> placeholdersConFormatoInvalido,
        List<String> advertencias,
        boolean tieneClausulasAdicionales
) {
}
