package org.mgroko.backend.common.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Generación del código estable de un tipo de contrato a partir de su nombre.
 *
 * <p>El código se genera automáticamente al crear el tipo y no se recalcula
 * cuando el nombre cambia. Es el identificador estable del catálogo y se usa
 * en filtros y búsquedas.</p>
 */
public final class CodigoTipoContrato {

    public static final int LONGITUD_MAXIMA = 50;
    private static final String BASE_POR_DEFECTO = "TIPO_CONTRATO";

    private CodigoTipoContrato() {
    }

    /**
     * Normaliza un nombre a un código en mayúsculas, sin acentos y con
     * separadores {@code _}.
     *
     * @param nombre nombre del tipo de contrato
     * @return código normalizado, nunca vacío y de hasta 50 caracteres
     */
    public static String normalizar(String nombre) {
        String texto = nombre == null ? "" : nombre.trim();
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        String codigo = sinAcentos.toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");

        if (codigo.isEmpty()) {
            codigo = BASE_POR_DEFECTO;
        }

        if (codigo.length() > LONGITUD_MAXIMA) {
            codigo = codigo.substring(0, LONGITUD_MAXIMA).replaceAll("_+$", "");
            if (codigo.isEmpty()) {
                codigo = BASE_POR_DEFECTO;
            }
        }

        return codigo;
    }

    /**
     * Recorta un código base para que, junto con un sufijo, no supere la
     * longitud máxima de la columna.
     *
     * @param base   código base
     * @param sufijo sufijo a agregar (incluye el guion bajo)
     * @return código base recortado
     */
    public static String recortarParaSufijo(String base, String sufijo) {
        int maximoBase = LONGITUD_MAXIMA - sufijo.length();
        if (maximoBase <= 0) {
            return BASE_POR_DEFECTO;
        }
        String recortado = base.length() > maximoBase
                ? base.substring(0, maximoBase).replaceAll("_+$", "")
                : base;
        return recortado.isEmpty() ? BASE_POR_DEFECTO : recortado;
    }
}
