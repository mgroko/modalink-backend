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
        PerfilConexionResponse perfil,
        boolean enLinea
) {

    /**
     * Constructor histórico para consumidores que todavía no leen presencia.
     */
    public ConexionResponse(
            Long idConexion,
            LocalDateTime fechaInicioConexion,
            Long idPerfil1,
            Long idPerfil2,
            PerfilConexionResponse perfil) {
        this(idConexion, fechaInicioConexion, idPerfil1, idPerfil2, perfil, false);
    }
}
