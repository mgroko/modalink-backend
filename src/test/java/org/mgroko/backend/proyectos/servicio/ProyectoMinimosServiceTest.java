package org.mgroko.backend.proyectos.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProyectoMinimosServiceTest {

    private static final Long ID_PROYECTO = 100L;

    @Mock
    private MiembroProyectoRepository miembroProyectoRepository;
    @Mock
    private RequerimientoGralProyectoRepository requerimientoGralRepository;
    @Mock
    private RequerimientoActividadRepository requerimientoActividadRepository;

    private ProyectoMinimosService service;

    @BeforeEach
    void setUp() {
        service = new ProyectoMinimosService(
                miembroProyectoRepository,
                requerimientoGralRepository,
                requerimientoActividadRepository);
    }

    @Test
    void tieneAlMenosDosMiembrosActivos_conDosActivos_esTrue() {
        when(miembroProyectoRepository
                .countByProyectoIdProyectoAndEstadoParticipacion(
                        ID_PROYECTO, EstadoParticipacion.ACTIVO))
                .thenReturn(2L);

        assertThat(service.tieneAlMenosDosMiembrosActivos(ID_PROYECTO)).isTrue();
    }

    @Test
    void tieneAlMenosDosMiembrosActivos_conSoloDirector_esFalse() {
        when(miembroProyectoRepository
                .countByProyectoIdProyectoAndEstadoParticipacion(
                        ID_PROYECTO, EstadoParticipacion.ACTIVO))
                .thenReturn(1L);

        assertThat(service.tieneAlMenosDosMiembrosActivos(ID_PROYECTO)).isFalse();
    }

    @Test
    void tieneRequerimientoComputable_conGeneralActivo_esTrue() {
        when(requerimientoGralRepository
                .existsByProyectoIdProyectoAndFechaArchivadoIsNull(ID_PROYECTO))
                .thenReturn(true);

        assertThat(service.tieneRequerimientoComputable(ID_PROYECTO)).isTrue();
    }

    @Test
    void tieneRequerimientoComputable_conActividadNoCancelada_esTrue() {
        when(requerimientoActividadRepository.existsByProyectoExcluyendoEstado(
                ID_PROYECTO, EstadoActividad.CANCELADA))
                .thenReturn(true);

        assertThat(service.tieneRequerimientoComputable(ID_PROYECTO)).isTrue();
    }

    @Test
    void tieneRequerimientoComputable_sinFuentes_esFalse() {
        assertThat(service.tieneRequerimientoComputable(ID_PROYECTO)).isFalse();
    }

    @Test
    void tieneRequerimientoHistorico_conGeneralArchivado_esTrue() {
        when(requerimientoGralRepository.existsByProyectoIdProyecto(ID_PROYECTO))
                .thenReturn(true);

        assertThat(service.tieneRequerimientoHistorico(ID_PROYECTO)).isTrue();
    }

    @Test
    void tieneRequerimientoHistorico_conActividadCancelada_esTrue() {
        when(requerimientoActividadRepository
                .existsByProyectoIncluyendoArchivadosYCancelados(ID_PROYECTO))
                .thenReturn(true);

        assertThat(service.tieneRequerimientoHistorico(ID_PROYECTO)).isTrue();
    }

    @Test
    void tieneRequerimientoHistorico_sinFuentes_esFalse() {
        assertThat(service.tieneRequerimientoHistorico(ID_PROYECTO)).isFalse();
    }
}
