package org.mgroko.backend.modelo.enums;

/**
 * Motivos predefinidos de UC-15. Los códigos coinciden exactamente con el
 * CHECK de {@code reporte.motivo}.
 */
public enum MotivoReporte {

    SPAM("Spam o publicidad engañosa"),
    CONTENIDO_OFENSIVO("Contenido ofensivo o inapropiado"),
    SUPLANTACION("Suplantación de identidad"),
    PERFIL_FALSO("Perfil falso o fraudulento"),
    OTRO("Otro");

    private final String descripcion;

    MotivoReporte(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
