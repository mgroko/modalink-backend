package org.mgroko.backend.reportes.enums;

/**
 * Motivos predefinidos de UC-69 para reportar una publicación.
 *
 * <p>El API y el formulario usan este catálogo específico. En la tabla
 * {@code reporte.motivo} se persiste el equivalente de {@code MotivoReporte}
 * para no ampliar el CHECK existente ni mezclar los motivos de perfil.</p>
 */
public enum MotivoReportePublicacion {

    SPAM("Spam o publicidad engañosa"),
    CONTENIDO_INAPROPIADO("Contenido inapropiado"),
    IDENTIDAD_FALSA("Identidad falsa"),
    OTRO("Otro");

    private final String descripcion;

    MotivoReportePublicacion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
