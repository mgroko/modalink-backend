package org.mgroko.backend.publicaciones.dto;

import java.time.LocalDate;

import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Filtros de UC-65. La búsqueda siempre se acota al perfil activo.
 *
 * <p>Si {@code estado} se omite, el servicio aplica {@code PENDIENTE}. La
 * dirección omitida incluye solicitudes recibidas y enviadas.</p>
 */
public record BuscarSolicitudesColaboracionFiltro(
        DireccionSolicitudColaboracion direccion,
        Long idPerfilEmisor,
        Long idPerfilReceptor,
        EstadoSolicitud estado,
        Boolean todosLosEstados,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
        Long idPublicacion
) {
}
