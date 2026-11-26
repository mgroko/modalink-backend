package org.mgroko.backend.repositorio.proyeccion;

import java.time.LocalDateTime;

/**
 * Miembro visible en la vista pública de un proyecto, con la versión de T&C
 * aceptada al incorporarse.
 */
public interface MiembroPublicoTycProyeccion {

    Long getIdPerfil();

    String getNombreArtistico();

    String getNombreProfesion();

    String getCodigoRol();

    Long getIdTycAceptado();

    String getDescripcion();

    LocalDateTime getFechaInicio();

    LocalDateTime getFechaFin();
}
