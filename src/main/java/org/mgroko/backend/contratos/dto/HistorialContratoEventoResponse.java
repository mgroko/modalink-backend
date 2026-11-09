package org.mgroko.backend.contratos.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Evento del historial de contratos de un proyecto.
 *
 * <p>El Director ve todos los eventos del proyecto. Un miembro ve unicamente
 * los eventos de los contratos donde su perfil es o fue parte firmante. Los
 * contratos eliminados y los de proyectos finalizados se incluyen porque el
 * historial es de solo lectura.</p>
 */
public record HistorialContratoEventoResponse(
        Long idEvento,
        Long idContrato,
        String nombreContrato,
        Long idProyecto,
        String nombreProyecto,
        Long idTipoAcuerdo,
        String codigoTipoAcuerdo,
        String nombreTipoAcuerdo,
        String tipoEvento,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime fechaHora,
        Long idMiembro,
        Long idPerfil,
        String nombreParte,
        Long idUsuario,
        String nombreOperador,
        Integer version,
        String motivo,
        String estadoContrato,
        String estadoProyecto,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime fechaBaja
) {
}
