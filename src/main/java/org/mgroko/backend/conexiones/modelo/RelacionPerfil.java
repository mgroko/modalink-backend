package org.mgroko.backend.conexiones.modelo;

/**
 * Estado de relación entre dos perfiles y los identificadores navegables.
 */
public record RelacionPerfil(
        EstadoRelacionPerfil estado,
        Long idSolicitudConexion,
        Long idConexion
) {

    public static RelacionPerfil ninguna() {
        return new RelacionPerfil(EstadoRelacionPerfil.NINGUNA, null, null);
    }
}
