package org.mgroko.backend.ubicacion.exception;

/**
 * Se lanza cuando se envía una provincia sin indicar la localidad correspondiente.
 * La localidad es obligatoria para resolver la ubicación completa.
 */
public class ProvinciaSinLocalidadException extends RuntimeException {

    public ProvinciaSinLocalidadException(String message) {
        super(message);
    }
}
