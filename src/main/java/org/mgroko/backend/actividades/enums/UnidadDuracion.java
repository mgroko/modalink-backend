package org.mgroko.backend.actividades.enums;

public enum UnidadDuracion {

    DIAS(1440),
    HORAS(60);

    private final int minutosPorUnidad;

    UnidadDuracion(int minutosPorUnidad) {
        this.minutosPorUnidad = minutosPorUnidad;
    }

    public long aMinutos(long cantidad) {
        return Math.multiplyExact(cantidad, minutosPorUnidad);
    }
}
