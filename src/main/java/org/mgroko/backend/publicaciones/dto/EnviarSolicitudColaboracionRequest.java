package org.mgroko.backend.publicaciones.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Body de UC-61. El remitente y la publicación se resuelven desde la sesión y
 * la ruta; solo se recibe el perfil destinatario.
 */
public record EnviarSolicitudColaboracionRequest(
        @NotNull(message = "El perfil destinatario es obligatorio.")
        @Positive(message = "El perfil destinatario debe ser válido.")
        Long idPerfilDestinatario
) {
}
