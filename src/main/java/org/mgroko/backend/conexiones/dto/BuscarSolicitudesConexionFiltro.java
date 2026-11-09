package org.mgroko.backend.conexiones.dto;

import java.time.LocalDate;

import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Filtros de UC-46. Todos son opcionales.
 *
 * <p>Si {@code estado} se omite, el servicio aplica {@code PENDIENTE}. La
 * dirección omitida incluye solicitudes recibidas y enviadas por el perfil
 * activo.</p>
 */
public record BuscarSolicitudesConexionFiltro(
        DireccionSolicitudConexion direccion,
        EstadoSolicitud estado,
        String nombre,
        String apellido,
        String nombreArtistico,
        Long idProfesion,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
        Boolean todosLosEstados
) {
}
