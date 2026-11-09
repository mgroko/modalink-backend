package org.mgroko.backend.conexiones.dto;

import java.time.LocalDateTime;

/**
 * Ítem de la red de contactos. `perfil` es siempre la contraparte del perfil
 * activo.
 */
public record ConexionResponse(
        Long idConexion,
        LocalDateTime fechaInicioConexion,
        Long idPerfil1,
        Long idPerfil2,
        PerfilConexionResponse perfil
) {
}
