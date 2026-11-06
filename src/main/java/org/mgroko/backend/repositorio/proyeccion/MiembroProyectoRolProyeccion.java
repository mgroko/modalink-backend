package org.mgroko.backend.repositorio.proyeccion;

/**
 * Rol activo del perfil en un proyecto, sin cargar la entidad completa.
 */
public interface MiembroProyectoRolProyeccion {

    Long getIdProyecto();

    String getCodigo();

    String getNombre();
}
