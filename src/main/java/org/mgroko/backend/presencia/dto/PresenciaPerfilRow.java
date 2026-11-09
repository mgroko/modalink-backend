package org.mgroko.backend.presencia.dto;

import java.time.LocalDateTime;

import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoUsuario;

/**
 * Proyección interna para calcular presencia sin cargar entidades completas.
 */
public record PresenciaPerfilRow(
        Long idPerfil,
        EstadoPerfil estado,
        EstadoUsuario estadoUsuario,
        LocalDateTime ultimaActividad
) {
}
