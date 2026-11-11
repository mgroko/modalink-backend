package org.mgroko.backend.reportes.mapper;

import org.mgroko.backend.modelo.enums.MotivoReporte;
import org.mgroko.backend.reportes.enums.MotivoReportePublicacion;

/**
 * Traduce el catálogo de motivos de UC-69 al enum persistido en
 * {@code reporte.motivo}.
 *
 * <p>La columna y el CHECK actuales se comparten con UC-15. Por eso
 * {@code CONTENIDO_INAPROPIADO} se guarda como {@code CONTENIDO_OFENSIVO} y
 * {@code IDENTIDAD_FALSA} como {@code SUPLANTACION}. El camino inverso se usa
 * para exponer la bandeja administrativa con los códigos de publicación.</p>
 */
public final class MotivoReportePublicacionMapper {

    private MotivoReportePublicacionMapper() {
    }

    public static MotivoReporte aPersistido(MotivoReportePublicacion motivo) {
        if (motivo == null) {
            return null;
        }

        return switch (motivo) {
            case SPAM -> MotivoReporte.SPAM;
            case CONTENIDO_INAPROPIADO -> MotivoReporte.CONTENIDO_OFENSIVO;
            case IDENTIDAD_FALSA -> MotivoReporte.SUPLANTACION;
            case OTRO -> MotivoReporte.OTRO;
        };
    }

    public static MotivoReportePublicacion desdePersistido(MotivoReporte motivo) {
        if (motivo == null) {
            return null;
        }

        return switch (motivo) {
            case SPAM -> MotivoReportePublicacion.SPAM;
            case CONTENIDO_OFENSIVO -> MotivoReportePublicacion.CONTENIDO_INAPROPIADO;
            case SUPLANTACION -> MotivoReportePublicacion.IDENTIDAD_FALSA;
            case PERFIL_FALSO, OTRO -> MotivoReportePublicacion.OTRO;
        };
    }

    public static String descripcionDe(MotivoReporte motivo) {
        MotivoReportePublicacion motivoPublicacion = desdePersistido(motivo);
        return motivoPublicacion != null ? motivoPublicacion.getDescripcion() : null;
    }
}
