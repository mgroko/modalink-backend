package org.mgroko.backend.proyectos.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.modelo.enums.RolProyectoUsuario;
import org.mgroko.backend.ubicacion.dto.UbicacionResponse;

public record ProyectoResponse(
        Long idProyecto,
        String nombre,
        String descripcion,
        LocalDate fechaInicio,
        LocalDate fechaEntregaEstipulada,
        String estado,
        String privacidad,
        Boolean aceptaPostulacionGral,
        UbicacionResponse ubicacion,
        Long idDirector,
        String nombreDirector,
        List<ObjetivoResponse> objetivos,
        List<RequerimientoGralResponse> requerimientosGral,
        MoodboardResponse moodboard,
        RolProyectoUsuario rolUsuario,
        Boolean esMiembro,
        List<String> permisos,
        Boolean elegibleParaPostularse,
        String motivoCancelacion,
        Long postulacionesPendientes,
        Boolean puedeDarseDeBaja,
        LocalDateTime fechaFinalizacion,
        LocalDateTime fechaCancelacion,
        Boolean finalizacionPrematura
) {

    /**
     * Constructor histórico para consumidores que todavía no leen la fecha de
     * cancelación.
     */
    public ProyectoResponse(
            Long idProyecto,
            String nombre,
            String descripcion,
            LocalDate fechaInicio,
            LocalDate fechaEntregaEstipulada,
            String estado,
            String privacidad,
            Boolean aceptaPostulacionGral,
            UbicacionResponse ubicacion,
            Long idDirector,
            String nombreDirector,
            List<ObjetivoResponse> objetivos,
            List<RequerimientoGralResponse> requerimientosGral,
            MoodboardResponse moodboard,
            RolProyectoUsuario rolUsuario,
            Boolean esMiembro,
            List<String> permisos,
            Boolean elegibleParaPostularse,
            String motivoCancelacion,
            Long postulacionesPendientes,
            Boolean puedeDarseDeBaja,
            LocalDateTime fechaFinalizacion,
            Boolean finalizacionPrematura) {
        this(
                idProyecto,
                nombre,
                descripcion,
                fechaInicio,
                fechaEntregaEstipulada,
                estado,
                privacidad,
                aceptaPostulacionGral,
                ubicacion,
                idDirector,
                nombreDirector,
                objetivos,
                requerimientosGral,
                moodboard,
                rolUsuario,
                esMiembro,
                permisos,
                elegibleParaPostularse,
                motivoCancelacion,
                postulacionesPendientes,
                puedeDarseDeBaja,
                fechaFinalizacion,
                null,
                finalizacionPrematura);
    }
}
