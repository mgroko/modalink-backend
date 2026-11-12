package org.mgroko.backend.publicaciones.dto;

/**
 * Contadores y estado de interacción de una publicación para el perfil activo.
 * Se resuelve en lote para los listados.
 */
public record ResumenInteraccionPublicacion(
        int cantidadMeGusta,
        int cantidadComentarios,
        boolean meGusta
) {

    private static final ResumenInteraccionPublicacion VACIO =
            new ResumenInteraccionPublicacion(0, 0, false);

    public static ResumenInteraccionPublicacion vacio() {
        return VACIO;
    }
}
