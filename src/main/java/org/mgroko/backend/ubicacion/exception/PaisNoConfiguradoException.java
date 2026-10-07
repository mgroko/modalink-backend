package org.mgroko.backend.ubicacion.exception;

/**
 * Se lanza cuando no se puede determinar a qué país pertenece una fuente de
 * catálogo geográfico: o falta la clave que lo declara en
 * {@code configuracion_sistema}, o el código ISO que esa clave declara no
 * corresponde a un país activo en la base.
 *
 * <p>Es una falla de configuración del sistema, no un error de los datos que
 * envía el cliente, por eso no se traduce a un error 400.</p>
 */
public class PaisNoConfiguradoException extends RuntimeException {

    public PaisNoConfiguradoException(String message) {
        super(message);
    }
}
