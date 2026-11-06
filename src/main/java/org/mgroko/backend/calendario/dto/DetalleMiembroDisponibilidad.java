package org.mgroko.backend.calendario.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.calendario.servicio.DisponibilidadCalculator.Intervalo;

/**
 * Resultado de disponibilidad de un miembro dentro de una evaluación común.
 *
 * <p>Incluye el perfil evaluado cuando el llamador lo conoce, para que el
 * detalle por miembro pueda mostrar el nombre artístico en lugar del ID de
 * usuario.</p>
 */
public record DetalleMiembroDisponibilidad(
        Long idUsuario,
        Long idPerfil,
        String nombreArtistico,
        LocalDateTime inicioEvaluado,
        LocalDateTime finEvaluado,
        List<Intervalo> huecos,
        boolean disponible,
        String motivo) {

    public DetalleMiembroDisponibilidad {
        huecos = huecos == null ? List.of() : List.copyOf(huecos);
    }

    /**
     * Constructor de compatibilidad para los flujos que todavía no tienen el
     * perfil evaluado.
     */
    public DetalleMiembroDisponibilidad(
            Long idUsuario,
            LocalDateTime inicioEvaluado,
            LocalDateTime finEvaluado,
            List<Intervalo> huecos,
            boolean disponible,
            String motivo) {
        this(idUsuario, null, null, inicioEvaluado, finEvaluado, huecos, disponible, motivo);
    }

    public DetalleMiembroDisponibilidad conPerfil(Long idPerfil, String nombreArtistico) {
        return new DetalleMiembroDisponibilidad(
                idUsuario,
                idPerfil,
                nombreArtistico,
                inicioEvaluado,
                finEvaluado,
                huecos,
                disponible,
                motivo);
    }
}
