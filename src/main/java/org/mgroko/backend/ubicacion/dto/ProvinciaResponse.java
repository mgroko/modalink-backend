package org.mgroko.backend.ubicacion.dto;

/**
 * Provincia del catálogo geográfico expuesta a la API.
 * Desacoplada de la fuente: el mismo contrato sirve para cualquier catálogo.
 */
public record ProvinciaResponse(String id, String nombre) {
}