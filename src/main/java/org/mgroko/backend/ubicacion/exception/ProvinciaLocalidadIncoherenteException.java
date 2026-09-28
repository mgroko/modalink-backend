package org.mgroko.backend.ubicacion.exception;

/**
 * Excepción lanzada cuando una localidad existe en el catálogo y tiene provincia,
 * pero la provincia indicada no coincide con la de la localidad.
 * Diferente a ProvinciaSinLocalidadException: aquí la localidad SÍ tiene provincia,
 * simplemente no es la correcta.
 */
public class ProvinciaLocalidadIncoherenteException extends RuntimeException {

    public ProvinciaLocalidadIncoherenteException(String message) {
        super(message);
    }
}