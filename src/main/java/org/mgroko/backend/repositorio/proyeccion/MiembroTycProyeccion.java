package org.mgroko.backend.repositorio.proyeccion;

import java.time.LocalDateTime;

/**
 * Términos y condiciones aceptados por un miembro activo de un proyecto.
 *
 * <p>Se proyecta la versión aceptada histórica (`id_tyc_aceptado`), no
 * necesariamente la versión vigente del perfil.</p>
 */
public interface MiembroTycProyeccion {

    Long getIdMiembro();

    Long getIdPerfil();

    String getNombreArtistico();

    String getNombreProfesion();

    Long getIdTycAceptado();

    Integer getNumeroVersion();

    String getDescripcion();

    LocalDateTime getFechaInicio();

    LocalDateTime getFechaFin();
}
