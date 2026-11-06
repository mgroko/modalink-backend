package org.mgroko.backend.reemplazos.servicio.modelo;

/**
 * Ubicación contra la cual se mide la proximidad de los candidatos.
 */
public record UbicacionEfectiva(
        Long idCiudad,
        Long idProvincia,
        String nombreCiudad,
        String nombreProvincia) {
}
