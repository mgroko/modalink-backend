package org.mgroko.backend.reemplazos.evento;

import org.mgroko.backend.reemplazos.enums.OrigenReemplazo;

/**
 * Evento que se publica cuando una membresía deja un proyecto confirmado.
 *
 * <p>El listener se ejecuta después del commit y reconstruye las vacantes a
 * partir del historial de la membresía. No se envían entidades JPA en el
 * evento para evitar acceder a asociaciones perezosas fuera de la transacción.</p>
 */
public record ReemplazoNecesarioEvent(
        Long idProyecto,
        Long idMiembroBaja,
        Long idPerfilBaja,
        OrigenReemplazo origen) {
}
