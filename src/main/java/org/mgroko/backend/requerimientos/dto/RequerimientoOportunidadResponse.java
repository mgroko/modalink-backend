package org.mgroko.backend.requerimientos.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.proyectos.dto.CaracteristicaRequerimientoResponse;
import org.mgroko.backend.ubicacion.dto.CiudadResponse;

/**
 * Fila común del buscador de oportunidades y del bloque de Home.
 *
 * <p>Los campos de actividad quedan en {@code null} para los requerimientos
 * generales.</p>
 *
 * <p>{@code ciudad} es la ubicación del proyecto, tanto para requerimientos
 * generales como para los de actividad. La actividad puede tener su propia
 * ubicación, pero es opcional y no se usa para el filtro de ubicación.</p>
 */
public record RequerimientoOportunidadResponse(
        TipoRequerimiento tipo,
        Long idRequerimiento,
        Integer cantidad,
        String descripcion,
        Long idProfesion,
        String nombreProfesion,
        Long idProyecto,
        String nombreProyecto,
        String estadoProyecto,
        Long idActividad,
        String nombreActividad,
        String estadoActividad,
        LocalDateTime fechaInicioProyecto,
        LocalDate fechaEntrega,
        LocalDateTime fechaHoraInicioActividad,
        LocalDateTime fechaHoraFinActividad,
        Integer duracionMinutos,
        Long cubiertos,
        Integer faltantes,
        Boolean satisfecho,
        List<CaracteristicaRequerimientoResponse> caracteristicas,
        List<Long> habilidades,
        Boolean puedePostularse,
        MotivoNoPostulable motivoNoPostulable,
        Boolean yaPostulado,
        EstadoSolicitud estadoPostulacion,
        Boolean requiereVerificacionDisponibilidad,
        CiudadResponse ciudad
) {

    public RequerimientoOportunidadResponse {
        caracteristicas = caracteristicas == null ? List.of() : List.copyOf(caracteristicas);
        habilidades = habilidades == null ? List.of() : List.copyOf(habilidades);
    }
}
