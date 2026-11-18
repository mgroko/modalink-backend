package org.mgroko.backend.publicaciones.evento;

import java.util.List;

/**
 * Solicita la detección automática de colaboraciones después del commit de la
 * transacción de subida.
 *
 * <p>Solo transporta identificadores: el trabajo asíncrono recarga la
 * publicación y las imágenes en una transacción propia.</p>
 */
public record ColaboracionAutomaticaSolicitadaEvent(
        Long idPublicacion,
        List<Long> idsImagenes) {

    public ColaboracionAutomaticaSolicitadaEvent {
        idsImagenes = idsImagenes == null ? List.of() : List.copyOf(idsImagenes);
    }
}
