package org.mgroko.backend.modelo.enums;

/**
 * Estados de una conexión de la red de contactos.
 *
 * <p>La baja es lógica: la conexión conserva su historial con estado
 * {@code ELIMINADA} y {@code fecha_fin_conexion}. El CHECK de la tabla
 * {@code conexion} acepta exactamente estos valores.</p>
 */
public enum EstadoConexion {
    ACTIVA,
    ELIMINADA;

    public static final EstadoConexion Activa = ACTIVA;
    public static final EstadoConexion Eliminada = ELIMINADA;

    public String getNombre() {
        return switch (this) {
            case ACTIVA -> "Activa";
            case ELIMINADA -> "Eliminada";
        };
    }
}
