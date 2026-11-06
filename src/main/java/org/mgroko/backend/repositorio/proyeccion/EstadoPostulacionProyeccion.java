package org.mgroko.backend.repositorio.proyeccion;

import org.mgroko.backend.modelo.enums.EstadoSolicitud;

/**
 * Estado de una postulación del perfil activo para un requerimiento concreto.
 * La clave {@code entidadId} es el id del requerimiento general o de actividad,
 * según el repositorio que produzca la proyección.
 */
public interface EstadoPostulacionProyeccion {

    Long getEntidadId();

    EstadoSolicitud getEstado();
}
