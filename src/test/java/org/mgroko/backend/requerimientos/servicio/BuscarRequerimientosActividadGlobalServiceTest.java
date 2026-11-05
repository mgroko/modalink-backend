package org.mgroko.backend.requerimientos.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.proyectos.servicio.CupoRequerimientoService;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PostulacionActividadRepository;
import org.mgroko.backend.repositorio.RequerimientoActCaractValorRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.mgroko.backend.requerimientos.dto.BuscarRequerimientosGlobalFiltro;
import org.mgroko.backend.requerimientos.dto.RequerimientoOportunidadResponse;
import org.mgroko.backend.requerimientos.dto.TipoRequerimiento;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class BuscarRequerimientosActividadGlobalServiceTest {

    private static final Long ID_PERFIL = 10L;
    private static final Long ID_PROFESION = 2L;
    private static final Long ID_PROYECTO = 5L;
    private static final Long ID_ACTIVIDAD = 40L;
    private static final Long ID_REQUERIMIENTO = 20L;

    @Mock
    private RequerimientoActividadRepository requerimientoActividadRepository;
    @Mock
    private RequerimientoActCaractValorRepository requerimientoActCaractValorRepository;
    @Mock
    private PostulacionActividadRepository postulacionActividadRepository;
    @Mock
    private MiembroProyectoRepository miembroProyectoRepository;
    @Mock
    private PerfilRepository perfilRepository;
    @Mock
    private CupoRequerimientoService cupoRequerimientoService;

    private BuscarRequerimientosActividadGlobalService servicio;

    @BeforeEach
    void setUp() {
        servicio = new BuscarRequerimientosActividadGlobalService(
                requerimientoActividadRepository,
                requerimientoActCaractValorRepository,
                postulacionActividadRepository,
                miembroProyectoRepository,
                perfilRepository,
                cupoRequerimientoService,
                new ElegibilidadRequerimientoService());
    }

    @Test
    void buscar_conPerfilActivo_devuelveOportunidadDeActividad() {
        Perfil perfil = Perfil.builder()
                .idPerfil(ID_PERFIL)
                .estado(EstadoPerfil.ACTIVO)
                .profesion(Profesion.builder()
                        .idProfesion(ID_PROFESION)
                        .nombre("Modelo")
                        .build())
                .build();

        Proyecto proyecto = Proyecto.builder()
                .idProyecto(ID_PROYECTO)
                .nombre("Colección Madoka")
                .estado(EstadoProyecto.PUBLICADO)
                .fechaInicio(LocalDateTime.of(2026, 11, 8, 9, 0))
                .build();

        Actividad actividad = Actividad.builder()
                .idActividad(ID_ACTIVIDAD)
                .nombre("Prueba de vestuario")
                .estado(EstadoActividad.PENDIENTE)
                .duracionMinutos(120)
                .fechaHoraInicio(LocalDateTime.of(2026, 11, 10, 9, 0))
                .planificacion(Planificacion.builder()
                        .idPlanificacion(50L)
                        .fechaEntrega(LocalDate.of(2026, 11, 20))
                        .proyecto(proyecto)
                        .build())
                .build();

        RequerimientoActividad requerimiento = RequerimientoActividad.builder()
                .idRequerimiento(ID_REQUERIMIENTO)
                .cantidad(1)
                .descripcion("Una modelo")
                .actividad(actividad)
                .profesion(perfil.getProfesion())
                .build();

        Pageable pageable = PageRequest.of(0, 20);
        when(perfilRepository.findByIdConProfesion(ID_PERFIL))
                .thenReturn(Optional.of(perfil));
        when(requerimientoActividadRepository.findAll(
                any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(requerimiento), pageable, 1));
        when(requerimientoActividadRepository.findConDetalleByIds(List.of(ID_REQUERIMIENTO)))
                .thenReturn(List.of(requerimiento));
        when(cupoRequerimientoService.calcularActividades(List.of(requerimiento)))
                .thenReturn(Map.of(ID_REQUERIMIENTO,
                        new CupoRequerimiento(1, 0, 1, false)));
        when(postulacionActividadRepository.findEstadosPorPerfilYRequerimientos(
                any(), anyCollection()))
                .thenReturn(List.of());
        when(miembroProyectoRepository.findRolesActivosPorPerfilYProyectos(
                anyCollection(), any()))
                .thenReturn(List.of());

        PaginaResponse<RequerimientoOportunidadResponse> respuesta = servicio.buscar(
                ID_PERFIL,
                new BuscarRequerimientosGlobalFiltro(
                        null, null, null, null, null, null, null, null, null, null),
                0, 20, false);

        RequerimientoOportunidadResponse fila = respuesta.contenido().getFirst();
        assertThat(fila.tipo()).isEqualTo(TipoRequerimiento.ACTIVIDAD);
        assertThat(fila.idActividad()).isEqualTo(ID_ACTIVIDAD);
        assertThat(fila.nombreActividad()).isEqualTo("Prueba de vestuario");
        assertThat(fila.estadoActividad()).isEqualTo("PENDIENTE");
        assertThat(fila.fechaEntrega()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(fila.faltantes()).isEqualTo(1);
        assertThat(fila.puedePostularse()).isTrue();
        assertThat(fila.requiereVerificacionDisponibilidad()).isTrue();
    }
}
