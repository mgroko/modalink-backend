package org.mgroko.backend.reemplazos.servicio.modelo;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import org.mgroko.backend.modelo.CaracteristicaTecnica;

/**
 * Característica técnica pedida por un requerimiento, normalizada para el
 * evaluador de concordancia.
 */
public record CaracteristicaRequerida(
        Long idCaracteristica,
        String codigo,
        String nombre,
        String tipoDato,
        BigDecimal valorMin,
        BigDecimal valorMax,
        Set<Long> idsValores,
        Map<Long, String> etiquetasValores) {

    public CaracteristicaRequerida {
        idsValores = idsValores == null ? Set.of() : Set.copyOf(idsValores);
        etiquetasValores = etiquetasValores == null
                ? Map.of()
                : Map.copyOf(etiquetasValores);
    }

    /**
     * Texto legible de lo que pidió la vacante para esta característica.
     *
     * <p>Para valores enumerados devuelve las etiquetas pedidas; para valores
     * numéricos devuelve el rango. Si no hay información suficiente devuelve
     * {@code null} y la UI usa su texto por defecto.</p>
     */
    public String valorPedido() {
        if (CaracteristicaTecnica.TIPO_ENUMERADO.equals(tipoDato)) {
            if (idsValores.isEmpty()) {
                return null;
            }
            String etiquetas = idsValores.stream()
                    .map(etiquetasValores::get)
                    .filter(java.util.Objects::nonNull)
                    .filter(texto -> !texto.isBlank())
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(java.util.stream.Collectors.joining(", "));
            return etiquetas.isBlank() ? null : etiquetas;
        }
        if (CaracteristicaTecnica.TIPO_NUMERICO.equals(tipoDato)) {
            return rango();
        }
        return null;
    }

    private String rango() {
        if (valorMin != null && valorMax != null) {
            return valorMin.stripTrailingZeros().toPlainString()
                    + " - "
                    + valorMax.stripTrailingZeros().toPlainString();
        }
        if (valorMin != null) {
            return "mínimo " + valorMin.stripTrailingZeros().toPlainString();
        }
        if (valorMax != null) {
            return "máximo " + valorMax.stripTrailingZeros().toPlainString();
        }
        return null;
    }
}
