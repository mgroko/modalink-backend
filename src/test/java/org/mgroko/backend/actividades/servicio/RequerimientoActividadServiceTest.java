package org.mgroko.backend.actividades.servicio;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.actividades.dto.RequerimientoActividadResponse;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.proyectos.servicio.CupoRequerimientoService;
import org.mgroko.backend.repositorio.CaracteristicaTecnicaRepository;
import org.mgroko.backend.repositorio.HabilidadRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
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

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private MiembroProyectoRepository miembroProyectoRepository;

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

    @Test
    void listarPorActividad_conPerfil_calculaPuedePostularseParaElDetalle() {
        Proyecto proyecto = Proyecto.builder()
                .idProyecto(5L)
                .estado(EstadoProyecto.PUBLICADO)
                .build();
        Actividad actividad = Actividad.builder()
                .idActividad(10L)
                .nombre("Montaje")
                .estado(EstadoActividad.PENDIENTE)
                .planificacion(Planificacion.builder()
                        .idPlanificacion(20L)
                        .proyecto(proyecto)
                        .build())
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
        Perfil perfil = Perfil.builder()
                .idPerfil(10L)
                .profesion(Profesion.builder()
                        .idProfesion(2L)
                        .nombre("Modelo")
                        .build())
                .build();

        when(perfilRepository.findByIdConProfesion(10L)).thenReturn(Optional.of(perfil));
        when(miembroProyectoRepository.findMiembroActivoConRol(5L, 10L))
                .thenReturn(Optional.empty());
        when(requerimientoActividadRepository.findByActividad_IdActividad(10L))
                .thenReturn(List.of(requerimiento));
        when(cupoRequerimientoService.calcularActividades(List.of(requerimiento)))
                .thenReturn(Map.of(7L, new CupoRequerimiento(2, 0, 2, false)));

        List<RequerimientoActividadResponse> respuesta =
                requerimientoActividadService.listarPorActividad(10L, 10L, proyecto);

        assertThat(respuesta).singleElement()
                .satisfies(r -> assertThat(r.puedePostularse()).isTrue());
    }
}
