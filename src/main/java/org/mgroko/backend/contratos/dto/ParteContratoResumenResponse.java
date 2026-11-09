package org.mgroko.backend.contratos.dto;

import java.time.LocalDateTime;

public record ParteContratoResumenResponse(
        Long idMiembro,
        Long idPerfil,
        String nombre,
        String rolParte,
        String estadoFirma,
        LocalDateTime fechaFirma
) {
}
