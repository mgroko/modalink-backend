package org.mgroko.backend.repositorio;

import java.util.Optional;

import org.mgroko.backend.modelo.ColaboracionAutomatica;

/**
 * Operaciones de persistencia que necesitan comportamiento atómico frente a la
 * carrera entre detecciones concurrentes de la misma pareja de publicaciones.
 */
public interface ColaboracionAutomaticaRepositoryCustom {

    /**
     * Inserta una propuesta pendiente solo si no existe otra para la misma
     * pareja canónica de publicaciones.
     *
     * @return el id generado si se insertó, o {@link Optional#empty()} si ya
     *         existía una propuesta pendiente para esa pareja.
     */
    Optional<Long> insertarPendienteSiNoExiste(ColaboracionAutomatica colaboracion);
}
