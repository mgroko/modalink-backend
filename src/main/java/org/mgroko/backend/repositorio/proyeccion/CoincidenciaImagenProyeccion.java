package org.mgroko.backend.repositorio.proyeccion;

/**
 * Fila de coincidencia entre una imagen recién subida y una publicación
 * existente.
 */
public interface CoincidenciaImagenProyeccion {

    Long getIdPublicacion();

    Long getIdImagen();

    String getSha256Contenido();

    Integer getDistancia();
}
