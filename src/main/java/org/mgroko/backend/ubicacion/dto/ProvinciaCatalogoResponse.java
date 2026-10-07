package org.mgroko.backend.ubicacion.dto;

/**
 * Provincia del catálogo geográfico expuesta a la API.
 * Desacoplada de la fuente: el mismo contrato sirve para cualquier catálogo.
 *
 * <p>No confundir con {@link ProvinciaResponse}, que proyecta la tabla
 * {@code provincia} de la base de datos. Esta es la proyeccion del catalogo
 * externo, esa es la proyeccion de la fila persistida.
 */
public record ProvinciaCatalogoResponse(String id, String nombre) {
}
