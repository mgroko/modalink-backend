package org.mgroko.backend.common.util;

import java.util.Locale;

/**
 * Construcción centralizada de patrones {@code LIKE} para búsquedas parciales
 * case-insensitive (hallazgo H-09).
 *
 * <p>Todo input de usuario que participe en un {@code LIKE} debe pasar por acá:
 * los caracteres {@code %} y {@code _} (y la propia barra de escape) se escapan
 * para que matcheen literal y no actúen como comodines. Cada query que use estos
 * patrones debe declarar {@code ESCAPE '\'} (JPQL) o usar
 * {@link jakarta.persistence.criteria.CriteriaBuilder#like(jakarta.persistence.criteria.Expression, String, char)}
 * con {@link #CARACTER_ESCAPE} (Specifications).
 *
 * <p>Propiedad: {@code contiene("%")} produce {@code "%\%%"}, distinto del
 * centinela {@code "%"} de "sin filtro", así que un {@code %} literal nunca se
 * confunde con "traer todo".
 */
public final class LikePatrones {

    /** Carácter de escape usado en los patrones y declarado en las queries. */
    public static final char CARACTER_ESCAPE = '\\';

    private LikePatrones() {}

    /**
     * Escapa los caracteres especiales de {@code LIKE} ({@code %}, {@code _} y
     * el propio carácter de escape). Recorta espacios; {@code null} es
     * {@code ""}.
     *
     * @param valor valor crudo de usuario (o {@code null})
     * @return valor con los caracteres especiales escapados, sin envolver
     */
    public static String escapar(String valor) {
        String texto = (valor == null) ? "" : valor.trim();
        StringBuilder resultado = new StringBuilder(texto.length());
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c == CARACTER_ESCAPE || c == '%' || c == '_') {
                resultado.append(CARACTER_ESCAPE);
            }
            resultado.append(c);
        }
        return resultado.toString();
    }

    /**
     * Arma un patrón "contiene" case-insensitive a partir de un valor crudo:
     * {@code null}/blanco es {@code "%"} (sin filtro); si no,
     * {@code "%" + escapar(minúsculas) + "%"}.
     *
     * @param valor valor crudo de usuario (o {@code null})
     * @return patrón {@code LIKE} listo para bindear
     */
    public static String contiene(String valor) {
        String texto = (valor == null) ? "" : valor.trim();
        if (texto.isEmpty()) {
            return "%";
        }
        return "%" + escapar(texto).toLowerCase(Locale.ROOT) + "%";
    }
}
