package org.mgroko.backend.repositorio.proyeccion;

/**
 * Proyección que vincula el id de una entidad (por ejemplo, una postulación)
 * con el id del proyecto al que pertenece.
 */
public interface ProyectoDeEntidad {

    Long getEntidadId();

    Long getIdProyecto();
}
