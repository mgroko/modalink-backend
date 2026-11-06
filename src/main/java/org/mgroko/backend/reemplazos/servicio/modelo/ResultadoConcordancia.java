package org.mgroko.backend.reemplazos.servicio.modelo;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado de comparar un perfil contra un requisito.
 *
 * <p>Las habilidades y características no excluyen: solo afectan el puntaje.
 * Cuando el perfil no cumple todo, {@code alternativo()} queda en true y
 * {@code faltantes()} describe qué falta.</p>
 */
public record ResultadoConcordancia(
        BigDecimal puntaje,
        boolean alternativo,
        List<String> faltantes) {

    public ResultadoConcordancia {
        faltantes = faltantes == null ? List.of() : List.copyOf(faltantes);
    }
}
