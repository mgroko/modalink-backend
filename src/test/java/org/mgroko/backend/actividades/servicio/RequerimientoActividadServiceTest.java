package org.mgroko.backend.actividades.servicio;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.actividades.dto.RequerimientoActividadResponse;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.proyectos.servicio.CupoRequerimientoService;
import org.mgroko.backend.repositorio.CaracteristicaTecnicaRepository;
import org.mgroko.backend.repositorio.HabilidadRepository;
import org.mgroko.backend.repositorio.ProfesionRepository;
import org.mgroko.backend.repositorio.RequerimientoActCaractValorRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.mgroko.backend.repositorio.ValorCaracteristicaRepository;
import static org.mockito.Mockito.when;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RequerimientoActividadServiceTest {

    @Mock
    private ProfesionRepository profesionRepository;

    @Mock
    private CaracteristicaTecnicaRepository caracteristicaTecnicaRepository;

    @Mock
    private ValorCaracteristicaRepository valorCaracteristicaRepository;

    @Mock
    private HabilidadRepository habilidadRepository;

    @Mock
    private RequerimientoActividadRepository requerimientoActividadRepository;

    @Mock
    private RequerimientoActCaractValorRepository requerimientoActCaractValorRepository;

    @Mock
    private CupoRequerimientoService cupoRequerimientoService;

    @InjectMocks
    private RequerimientoActividadService requerimientoActividadService;

    @Test
    void listarPorActividad_incluyeCupoYNoHabilitaPostulacionEnVistaDeEdicion() {
        Actividad actividad = Actividad.builder()
                .idActividad(10L)
                .nombre("Montaje")
                .build();
        RequerimientoActividad requerimiento = RequerimientoActividad.builder()
                .idRequerimiento(7L)
                .cantidad(2)
                .descripcion("Dos modelos")
                .profesion(Profesion.builder()
                        .idProfesion(2L)
                        .nombre("Modelo")
                        .build())
                .actividad(actividad)
                .build();

        when(requerimientoActividadRepository.findByActividad_IdActividad(10L))
                .thenReturn(List.of(requerimiento));
        when(cupoRequerimientoService.calcularActividades(List.of(requerimiento)))
                .thenReturn(Map.of(7L, new CupoRequerimiento(2, 1, 1, false)));

        List<RequerimientoActividadResponse> respuesta =
                requerimientoActividadService.listarPorActividad(10L);

        assertThat(respuesta).singleElement().satisfies(r -> {
            assertThat(r.idRequerimiento()).isEqualTo(7L);
            assertThat(r.cubiertos()).isEqualTo(1L);
            assertThat(r.faltantes()).isEqualTo(1);
            assertThat(r.satisfecho()).isFalse();
            assertThat(r.puedePostularse()).isFalse();
        });
    }
}
