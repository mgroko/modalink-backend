package org.mgroko.backend.conexiones.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Body de UC-43. El perfil remitente se toma del perfil activo de la sesión.
 */
public record EnviarSolicitudConexionRequest(
        @NotNull(message = "El perfil destinatario es obligatorio.")
        @Positive(message = "El perfil destinatario debe ser válido.")
        Long idPerfilDestinatario
) {
}
