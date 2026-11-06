package org.mgroko.backend.repositorio.proyeccion;

/**
 * Proyección con la cantidad de asignaciones activas de un requerimiento de
 * actividad.
 */
public interface CubiertosPorRequerimiento {

    Long getIdRequerimiento();

    Long getCubiertos();
}
