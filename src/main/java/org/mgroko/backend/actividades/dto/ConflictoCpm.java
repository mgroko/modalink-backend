package org.mgroko.backend.actividades.dto;

/**
 * Conflicto estructural detectado por el motor CPM.
 *
 * <p>Un conflicto no es lo mismo que una holgura negativa: describe datos que
 * impiden calcular un cronograma coherente (una actividad fija fuera del
 * proyecto, una predecesora que termina después del inicio fijo de su
 * sucesora, o una actividad ya ejecutada sin inicio registrado). Las holguras
 * negativas se exponen en {@link NodoCpm} y hacen que
 * {@link CalculoCpm#viable()} sea {@code false}.</p>
 *
 * @param idActividad actividad afectada
 * @param tipo        clase de conflicto
 * @param detalle     explicación legible para el usuario
 */
public record ConflictoCpm(Long idActividad, Tipo tipo, String detalle) {

    public enum Tipo {
        /** La actividad fija empieza antes del inicio del proyecto. */
        FUERA_DE_PROYECTO,
        /** Una predecesora termina después del inicio fijo de la actividad. */
        CONFLICTO_FIJO,
        /** Falta un dato imprescindible (por ejemplo, el inicio de una actividad ya ejecutada). */
        DATO_INCONSISTENTE
    }

    public ConflictoCpm {
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de conflicto es obligatorio.");
        }
        if (detalle == null || detalle.isBlank()) {
            throw new IllegalArgumentException("El detalle del conflicto es obligatorio.");
        }
    }
}
