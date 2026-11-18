package org.mgroko.backend.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.proyectos.dto.MoodboardResponse;
import org.mgroko.backend.proyectos.dto.ObjetivoResponse;
import org.mgroko.backend.ubicacion.dto.UbicacionResponse;

/**
 * Detalle administrativo de un proyecto. No incluye flags de accion porque el
 * administrador solo consulta.
 */
public record AdminProyectoDetalleResponse(
        Long idProyecto,
        String nombre,
        String descripcion,
        String estado,
        String privacidad,
        Boolean aceptaPostulacionGral,
        LocalDateTime fechaInicio,
        LocalDate fechaEntregaEstipulada,
        LocalDateTime fechaFinalizacion,
        LocalDateTime fechaCancelacion,
        String motivoCancelacion,
        Boolean finalizacionPrematura,
        UbicacionResponse ubicacion,
        DirectorResponse director,
        List<ObjetivoResponse> objetivos,
        List<RequerimientoResponse> requerimientosGral,
        MoodboardResponse moodboard,
        List<MiembroResponse> miembros,
        List<ContratoResumen> contratos,
        long totalContratos
) {

    public record DirectorResponse(
            Long idPerfil,
            String nombreArtistico,
            String nombreCompleto
    ) {
    }

    public record RequerimientoResponse(
            Long idRequerimientoGral,
            Integer cantidad,
            String descripcion,
            Long idProfesion,
            String nombreProfesion,
            List<CaracteristicaResponse> caracteristicas,
            List<String> habilidades
    ) {
    }

    public record CaracteristicaResponse(
            Long idCaracteristica,
            String codigo,
            String tipoDato,
            BigDecimal valorMin,
            BigDecimal valorMax
    ) {
    }

    public record MiembroResponse(
            Long idMiembro,
            Long idPerfil,
            String nombreArtistico,
            String nombreCompleto,
            String profesion,
            String rolProyecto,
            String estadoParticipacion,
            LocalDateTime fechaAlta,
            LocalDateTime fechaBaja,
            String motivoBaja
    ) {
    }

    public record ContratoResumen(
            Long idContrato,
            String nombre,
            String estado,
            Integer versionActual,
            LocalDateTime fechaCreacion,
            LocalDateTime fechaEmision,
            int firmados,
            int totalPartes
    ) {
    }
}
