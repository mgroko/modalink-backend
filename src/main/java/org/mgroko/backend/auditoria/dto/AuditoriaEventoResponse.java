package org.mgroko.backend.auditoria.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AuditoriaEventoResponse(
        Long idAuditoria,
        LocalDateTime fechaHora,
        String tipoEvento,
        String tipoEventoNombre,
        String entidadAfectada,
        String entidadClave,
        Long idAfectado,
        Long idUsuario,
        String usuarioNombre,
        String usuarioBd,
        String ipUsuario,
        String tipoEventoOriginal,
        boolean legacy,
        String operador,
        List<AuditoriaDetalleResponse> detalle
) {
}
