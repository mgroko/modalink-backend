package org.mgroko.backend.postulaciones.casting.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.postulaciones.dto.TipoPostulacion;

public record CandidatoCastingResponse(
        Long idPostulacion,
        TipoPostulacion tipo,
        EstadoSolicitud estado,
        LocalDateTime fechaPostulacion,
        Long idPerfil,
        String nombreArtistico,
        Long idProfesion,
        String codigoProfesion,
        String nombreProfesion,
        String fotoUrl,
        String ciudad,
        String provincia,
        boolean aportaCupo,
        boolean puedeAceptar,
        boolean puedeRechazar,
        List<CaracteristicaPerfilCastingResponse> caracteristicas,
        List<PolaroidCastingResponse> polaroids,
        CumplimientoCastingResponse cumplimiento) {

    public CandidatoCastingResponse {
        caracteristicas = caracteristicas == null ? List.of() : List.copyOf(caracteristicas);
        polaroids = polaroids == null ? List.of() : List.copyOf(polaroids);
    }
}
