package org.mgroko.backend.common.util;

import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;

/**
 * Resuelve el nombre de una profesión según el género de la persona.
 *
 * <p>Regla del sistema: solo {@code MUJER} usa la forma femenina. Para
 * {@code HOMBRE}, {@code NO_BINARIO}, {@code NO_DECIRLO} o cualquier valor
 * ausente se devuelve el nombre canónico (masculino genérico).</p>
 */
public final class ProfesionNombreResolver {

    private static final String CODIGO_MUJER = "MUJER";

    private ProfesionNombreResolver() {
    }

    /**
     * Resuelve el nombre para el perfil indicado usando el género de su usuario.
     * Es tolerante a perfiles sin usuario o sin género (por ejemplo, mocks y
     * datos históricos anonimizados).
     */
    public static String mostrar(Perfil perfil) {
        if (perfil == null) {
            return null;
        }
        Genero genero = perfil.getUsuario() != null
                ? perfil.getUsuario().getGenero()
                : null;
        return mostrar(perfil.getProfesion(), genero);
    }

    /**
     * Resuelve el nombre de la profesión para el género indicado.
     */
    public static String mostrar(Profesion profesion, Genero genero) {
        if (profesion == null) {
            return null;
        }

        boolean esMujer = genero != null
                && genero.getCodigo() != null
                && CODIGO_MUJER.equalsIgnoreCase(genero.getCodigo());

        if (esMujer
                && profesion.getNombreFemenino() != null
                && !profesion.getNombreFemenino().isBlank()) {
            return profesion.getNombreFemenino();
        }

        return profesion.getNombre();
    }
}
