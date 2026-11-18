package org.mgroko.backend.publicaciones.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Solicitud para etiquetar un perfil propio como colaborador de una
 * publicación propia.
 */
public record AgregarColaboradorPropioRequest(
        @NotNull Long idPerfil) {
}
