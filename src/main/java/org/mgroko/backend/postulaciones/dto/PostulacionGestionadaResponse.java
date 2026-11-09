package org.mgroko.backend.postulaciones.dto;

import java.util.List;

import org.mgroko.backend.calendario.dto.ResultadoPreferencia;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;

/**
 * Resultado de aceptar o rechazar una postulación.
 */
public record PostulacionGestionadaResponse(
        Long idPostulacion,
        TipoPostulacion tipo,
        EstadoSolicitud estado,
        Long idProyecto,
        Long idRequerimiento,
        Long idPerfil,
        Long idMiembro,
        Long idAsignacionActividad,
        ResultadoPreferencia resultadoPreferencia,
        Long desvioMinutos,
        List<AdvertenciaSobrecompromiso> advertencias,
        Long idTycAceptado) {

    /**
     * Constructor histórico para respuestas de rechazo o consumidores que aún
     * no leen los datos de preferencia y sobrecompromiso.
     */
    public PostulacionGestionadaResponse(
            Long idPostulacion,
            TipoPostulacion tipo,
            EstadoSolicitud estado,
            Long idProyecto,
            Long idRequerimiento,
            Long idPerfil,
            Long idMiembro,
            Long idAsignacionActividad) {
        this(
                idPostulacion,
                tipo,
                estado,
                idProyecto,
                idRequerimiento,
                idPerfil,
                idMiembro,
                idAsignacionActividad,
                null,
                null,
                List.of(),
                null);
    }

    /**
     * Constructor histórico para respuestas que ya informan preferencia y
     * sobrecompromiso, pero todavía no leen la versión de T&C aceptada.
     */
    public PostulacionGestionadaResponse(
            Long idPostulacion,
            TipoPostulacion tipo,
            EstadoSolicitud estado,
            Long idProyecto,
            Long idRequerimiento,
            Long idPerfil,
            Long idMiembro,
            Long idAsignacionActividad,
            ResultadoPreferencia resultadoPreferencia,
            Long desvioMinutos,
            List<AdvertenciaSobrecompromiso> advertencias) {
        this(
                idPostulacion,
                tipo,
                estado,
                idProyecto,
                idRequerimiento,
                idPerfil,
                idMiembro,
                idAsignacionActividad,
                resultadoPreferencia,
                desvioMinutos,
                advertencias,
                null);
    }

    public PostulacionGestionadaResponse {
        advertencias = advertencias == null ? List.of() : List.copyOf(advertencias);
    }
}
