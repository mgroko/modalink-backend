package org.mgroko.backend.reemplazos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.perfiles.dto.CaracteristicaResponse;
import org.mgroko.backend.reemplazos.enums.EstadoCandidatoReemplazo;
import org.mgroko.backend.reemplazos.enums.NivelUbicacion;
import org.mgroko.backend.reemplazos.enums.TipoDisponibilidad;
import org.mgroko.backend.ubicacion.dto.CiudadResponse;

public record CandidatoReemplazoResponse(
        Long idCandidato,
        Long idPerfil,
        Long idUsuario,
        String nombreArtistico,
        String nombre,
        String apellido,
        String profesion,
        String fotoUrl,
        CiudadResponse ciudad,
        List<String> habilidades,
        List<CaracteristicaResponse> caracteristicas,
        BigDecimal puntaje,
        NivelUbicacion nivelUbicacion,
        boolean alternativo,
        List<String> faltantes,
        TipoDisponibilidad tipoDisponibilidad,
        LocalDateTime inicioPropuesto,
        LocalDateTime finPropuesto,
        EstadoCandidatoReemplazo estado,
        String mensaje,
        LocalDateTime fechaInvitacion,
        LocalDateTime fechaRespuesta,
        LocalDateTime fechaDecision,
        String motivoDecision) {
}
