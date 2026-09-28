package org.mgroko.backend.ubicacion.exception;

/**
 * Se lanza cuando una localidad del catálogo llega sin provincia. La provincia
 * es obligatoria: cuelga del país ({@code provincia.id_pais} es NOT NULL) y la
 * jerarquía Pais -> Provincia -> Ciudad -> Ubicacion no admite ciudades
 * huérfanas.
 */
public class LocalidadSinProvinciaException extends RuntimeException {

    public LocalidadSinProvinciaException(String message) {
        super(message);
    }
}
