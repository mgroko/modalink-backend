package org.mgroko.backend.postulaciones.casting.dto;

import java.time.LocalDateTime;

public record AgendaCastingResponse(
        boolean evaluada,
        Boolean cumple,
        String estado,
        LocalDateTime inicio,
        LocalDateTime fin,
        String motivo) {

    public static AgendaCastingResponse noAplica() {
        return new AgendaCastingResponse(false, null, "NO_APLICA", null, null, null);
    }

    public static AgendaCastingResponse sinEvaluar(String motivo) {
        return new AgendaCastingResponse(false, null, "SIN_EVALUAR", null, null, motivo);
    }
}
