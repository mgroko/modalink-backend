package org.mgroko.backend.reportes.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.modelo.enums.MotivoReporte;
import org.mgroko.backend.reportes.enums.MotivoReportePublicacion;

class MotivoReportePublicacionMapperTest {

    @Test
    void aPersistido_mapeaTodosLosMotivos() {
        assertThat(MotivoReportePublicacionMapper.aPersistido(
                MotivoReportePublicacion.SPAM)).isEqualTo(MotivoReporte.SPAM);
        assertThat(MotivoReportePublicacionMapper.aPersistido(
                MotivoReportePublicacion.CONTENIDO_INAPROPIADO))
                .isEqualTo(MotivoReporte.CONTENIDO_OFENSIVO);
        assertThat(MotivoReportePublicacionMapper.aPersistido(
                MotivoReportePublicacion.IDENTIDAD_FALSA))
                .isEqualTo(MotivoReporte.SUPLANTACION);
        assertThat(MotivoReportePublicacionMapper.aPersistido(
                MotivoReportePublicacion.OTRO)).isEqualTo(MotivoReporte.OTRO);
    }

    @Test
    void desdePersistido_mapeaTodosLosMotivosDePublicacion() {
        assertThat(MotivoReportePublicacionMapper.desdePersistido(MotivoReporte.SPAM))
                .isEqualTo(MotivoReportePublicacion.SPAM);
        assertThat(MotivoReportePublicacionMapper.desdePersistido(
                MotivoReporte.CONTENIDO_OFENSIVO))
                .isEqualTo(MotivoReportePublicacion.CONTENIDO_INAPROPIADO);
        assertThat(MotivoReportePublicacionMapper.desdePersistido(
                MotivoReporte.SUPLANTACION))
                .isEqualTo(MotivoReportePublicacion.IDENTIDAD_FALSA);
        assertThat(MotivoReportePublicacionMapper.desdePersistido(MotivoReporte.OTRO))
                .isEqualTo(MotivoReportePublicacion.OTRO);
    }

    @Test
    void desdePersistido_perfilFalso_caeEnOtro() {
        assertThat(MotivoReportePublicacionMapper.desdePersistido(
                MotivoReporte.PERFIL_FALSO)).isEqualTo(MotivoReportePublicacion.OTRO);
    }

    @Test
    void nulos_devuelvenNulo() {
        assertThat(MotivoReportePublicacionMapper.aPersistido(null)).isNull();
        assertThat(MotivoReportePublicacionMapper.desdePersistido(null)).isNull();
        assertThat(MotivoReportePublicacionMapper.descripcionDe(null)).isNull();
    }
}
