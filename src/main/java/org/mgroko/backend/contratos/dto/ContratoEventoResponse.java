package org.mgroko.backend.contratos.dto;

import java.time.LocalDateTime;

/**
 * Evento del historial de un contrato, visible para sus partes.
 */
public record ContratoEventoResponse(
        Long idEvento,
        String tipoEvento,
        LocalDateTime fechaHora,
        Long idMiembro,
        Long idPerfil,
        String nombreParte,
        Long idUsuario,
        String nombreOperador,
        Integer version,
        String motivo
) {
}
