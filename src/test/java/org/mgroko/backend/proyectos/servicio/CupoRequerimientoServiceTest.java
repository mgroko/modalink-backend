package org.mgroko.backend.proyectos.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CupoRequerimientoServiceTest {

    @Mock
    private MiembroProyectoRepository miembroProyectoRepository;

    @Mock
    private AsignacionActividadRepository asignacionActividadRepository;

    @InjectMocks
    private CupoRequerimientoService cupoRequerimientoService;

    @Test
    void general_parcial_calculaFaltantesYNoSatisfecho() {
        RequerimientoGralProyecto requerimiento = RequerimientoGralProyecto.builder()
                .idRequerimientoGral(20L)
                .cantidad(2)
                .build();
        when(miembroProyectoRepository.countActivosPorRequerimientoGral(20L)).thenReturn(1L);

        CupoRequerimiento cupo = cupoRequerimientoService.calcularGeneral(requerimiento);

        assertThat(cupo.cantidad()).isEqualTo(2);
        assertThat(cupo.cubiertos()).isEqualTo(1L);
        assertThat(cupo.faltantes()).isEqualTo(1);
        assertThat(cupo.satisfecho()).isFalse();
        verify(miembroProyectoRepository).countActivosPorRequerimientoGral(20L);
    }

    @Test
    void general_completo_quedaSatisfecho() {
        RequerimientoGralProyecto requerimiento = RequerimientoGralProyecto.builder()
                .idRequerimientoGral(20L)
                .cantidad(2)
                .build();
        when(miembroProyectoRepository.countActivosPorRequerimientoGral(20L)).thenReturn(2L);

        CupoRequerimiento cupo = cupoRequerimientoService.calcularGeneral(requerimiento);

        assertThat(cupo.cubiertos()).isEqualTo(2L);
        assertThat(cupo.faltantes()).isZero();
        assertThat(cupo.satisfecho()).isTrue();
    }

    @Test
    void general_conExceso_noGeneraFaltantesNegativos() {
        RequerimientoGralProyecto requerimiento = RequerimientoGralProyecto.builder()
                .idRequerimientoGral(20L)
                .cantidad(2)
                .build();
        when(miembroProyectoRepository.countActivosPorRequerimientoGral(20L)).thenReturn(3L);

        CupoRequerimiento cupo = cupoRequerimientoService.calcularGeneral(requerimiento);

        assertThat(cupo.cubiertos()).isEqualTo(3L);
        assertThat(cupo.faltantes()).isZero();
        assertThat(cupo.satisfecho()).isTrue();
    }

    @Test
    void general_sinRequerimiento_devuelveCupoVacio() {
        CupoRequerimiento cupo = cupoRequerimientoService.calcularGeneral(null);

        assertThat(cupo.cantidad()).isZero();
        assertThat(cupo.cubiertos()).isZero();
        assertThat(cupo.faltantes()).isZero();
        assertThat(cupo.satisfecho()).isTrue();
    }

    @Test
    void actividad_parcial_calculaFaltantes() {
        RequerimientoActividad requerimiento = RequerimientoActividad.builder()
                .idRequerimiento(30L)
                .cantidad(2)
                .build();
        when(asignacionActividadRepository.countActivasPorRequerimientoActividad(30L)).thenReturn(1L);

        CupoRequerimiento cupo = cupoRequerimientoService.calcularActividad(requerimiento);

        assertThat(cupo.cantidad()).isEqualTo(2);
        assertThat(cupo.cubiertos()).isEqualTo(1L);
        assertThat(cupo.faltantes()).isEqualTo(1);
        assertThat(cupo.satisfecho()).isFalse();
        verify(asignacionActividadRepository).countActivasPorRequerimientoActividad(30L);
    }

    @Test
    void actividad_completa_quedaSatisfecha() {
        RequerimientoActividad requerimiento = RequerimientoActividad.builder()
                .idRequerimiento(30L)
                .cantidad(1)
                .build();
        when(asignacionActividadRepository.countActivasPorRequerimientoActividad(30L)).thenReturn(1L);

        CupoRequerimiento cupo = cupoRequerimientoService.calcularActividad(requerimiento);

        assertThat(cupo.faltantes()).isZero();
        assertThat(cupo.satisfecho()).isTrue();
    }
}
