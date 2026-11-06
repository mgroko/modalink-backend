package org.mgroko.backend.reemplazos.servicio.modelo;

import org.mgroko.backend.reemplazos.enums.NivelUbicacion;

/**
 * Candidato listo para persistir, con el detalle del ranking ya calculado.
 */
public record CandidatoEvaluado(
        CandidatoPerfil perfil,
        ResultadoConcordancia concordancia,
        NivelUbicacion nivelUbicacion,
        DisponibilidadCandidato disponibilidad) {

    public boolean disponible() {
        return disponibilidad != null && disponibilidad.disponible();
    }
}
