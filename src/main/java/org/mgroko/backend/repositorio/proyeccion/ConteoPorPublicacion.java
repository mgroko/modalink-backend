package org.mgroko.backend.repositorio.proyeccion;

/**
 * Proyección de conteo agrupado por publicación. Se usa para resolver en una
 * sola consulta los contadores de Me gusta y comentarios de una página.
 */
public interface ConteoPorPublicacion {

    Long getIdPublicacion();

    Long getTotal();
}
