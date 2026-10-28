package org.mgroko.backend.actividades.servicio;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.actividades.dto.ActividadBusquedaResponse;
import org.mgroko.backend.actividades.dto.BuscarActividadesFiltro;
import org.mgroko.backend.actividades.exception.RangoDuracionInvalidoException;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.servicio.CupoRequerimientoService;
import org.mgroko.backend.proyectos.servicio.ProyectoVisibilidadService;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class BuscarActividadServiceTest {

    @Mock
    private ActividadRepository actividadRepository;

    @Mock
    private ProyectoRepository proyectoRepository;

    @Mock
    private RequerimientoActividadRepository requerimientoActividadRepository;

    @Mock
    private ProyectoVisibilidadService proyectoVisibilidadService;

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private MiembroProyectoRepository miembroProyectoRepository;

    @Mock
    private CupoRequerimientoService cupoRequerimientoService;

    @InjectMocks
    private BuscarActividadService buscarActividadService;

    private Proyecto proyecto;
    private Actividad actividad;

    @BeforeEach
    void setUp() {
        proyecto = Proyecto.builder()
                .idProyecto(100L)
                .nombre("Desfile Primavera")
                .estado(EstadoProyecto.PUBLICADO)
                .build();

        Planificacion planificacion = Planificacion.builder()
                .idPlanificacion(50L)
                .proyecto(proyecto)
                .build();

        actividad = Actividad.builder()
                .idActividad(10L)
                .nombre("Montaje")
                .descripcion("Montar escenario")
                .duracionMinutos(90)
                .fechaHoraInicio(LocalDateTime.of(2026, 11, 1, 9, 0))
                .estado(EstadoActividad.PENDIENTE)
                .planificacion(planificacion)
                .build();
    }

    @Test
    void buscar_sinPerfilActivo_lanzaExcepcion() {
        assertThatThrownBy(() -> buscarActividadService.buscar(
                null, 100L, null, 0, 20, false))
                .isInstanceOf(PerfilActivoNoSeleccionadoException.class);

        verify(proyectoRepository, never()).findById(any());
    }

    @Test
    void buscar_rangoDuracionInvalido_lanzaExcepcion() {
        BuscarActividadesFiltro filtro = new BuscarActividadesFiltro(
                null, null, null, 120, 60, null);

        assertThatThrownBy(() -> buscarActividadService.buscar(
                1L, 100L, filtro, 0, 20, false))
                .isInstanceOf(RangoDuracionInvalidoException.class);

        verify(proyectoRepository, never()).findById(any());
    }

    @Test
    void buscar_duracionNegativa_lanzaExcepcion() {
        BuscarActividadesFiltro filtro = new BuscarActividadesFiltro(
                null, null, null, -1, null, null);

        assertThatThrownBy(() -> buscarActividadService.buscar(
                1L, 100L, filtro, 0, 20, false))
                .isInstanceOf(RangoDuracionInvalidoException.class);
    }

    @Test
    void buscar_proyectoInexistente_lanzaExcepcion() {
        when(proyectoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> buscarActividadService.buscar(
                1L, 999L, null, 0, 20, false))
                .isInstanceOf(ProyectoNoEncontradoException.class);
    }

    @Test
    void buscar_proyectoSinAcceso_lanzaExcepcion() {
        when(proyectoRepository.findById(100L)).thenReturn(Optional.of(proyecto));
        doThrow(new AccesoDenegadoProyectoException("Sin acceso"))
                .when(proyectoVisibilidadService).validarAcceso(proyecto, 1L);

        assertThatThrownBy(() -> buscarActividadService.buscar(
                1L, 100L, null, 0, 20, false))
                .isInstanceOf(AccesoDenegadoProyectoException.class);
    }

    @Test
    void buscar_devuelvePaginaEnriquecidaConCupoYElegibilidad() {
        RequerimientoActividad requerimiento = requerimiento(7L, 2, 2L);
        prepararBusqueda(
                perfilConProfesion(2L),
                null,
                List.of(requerimiento),
                Map.of(7L, new CupoRequerimiento(2, 0, 2, false)));

        Actividad predecesora = Actividad.builder()
                .idActividad(5L)
                .nombre("Casting")
                .build();
        actividad.setPredecesoras(Set.of(predecesora));

        PaginaResponse<ActividadBusquedaResponse> respuesta = buscarActividadService.buscar(
                1L, 100L, null, 0, 20, false);

        assertThat(respuesta.totalElementos()).isEqualTo(1);
        assertThat(respuesta.contenido()).hasSize(1);

        ActividadBusquedaResponse fila = respuesta.contenido().getFirst();
        assertThat(fila.idActividad()).isEqualTo(10L);
        assertThat(fila.idProyecto()).isEqualTo(100L);
        assertThat(fila.nombreProyecto()).isEqualTo("Desfile Primavera");
        assertThat(fila.estado()).isEqualTo("PENDIENTE");
        assertThat(fila.fechaHoraFin()).isEqualTo(LocalDateTime.of(2026, 11, 1, 10, 30));
        assertThat(fila.requerimientos()).singleElement()
                .satisfies(r -> {
                    assertThat(r.idRequerimiento()).isEqualTo(7L);
                    assertThat(r.nombreProfesion()).isEqualTo("Modelo");
                    assertThat(r.cubiertos()).isZero();
                    assertThat(r.faltantes()).isEqualTo(2);
                    assertThat(r.satisfecho()).isFalse();
                    assertThat(r.puedePostularse()).isTrue();
                });
        assertThat(fila.predecesoras()).singleElement()
                .satisfies(p -> {
                    assertThat(p.idActividad()).isEqualTo(5L);
                    assertThat(p.nombre()).isEqualTo("Casting");
                });

        verify(proyectoVisibilidadService).validarAcceso(proyecto, 1L);
    }

    @Test
    void buscar_requerimientoSatisfecho_noPuedePostularse() {
        RequerimientoActividad requerimiento = requerimiento(7L, 2, 2L);
        prepararBusqueda(
                perfilConProfesion(2L),
                null,
                List.of(requerimiento),
                Map.of(7L, new CupoRequerimiento(2, 2, 0, true)));

        PaginaResponse<ActividadBusquedaResponse> respuesta = buscarActividadService.buscar(
                1L, 100L, null, 0, 20, false);

        assertThat(respuesta.contenido().getFirst().requerimientos()).singleElement()
                .satisfies(r -> {
                    assertThat(r.cubiertos()).isEqualTo(2L);
                    assertThat(r.faltantes()).isZero();
                    assertThat(r.satisfecho()).isTrue();
                    assertThat(r.puedePostularse()).isFalse();
                });
    }

    @Test
    void buscar_actividadNoPendiente_noPuedePostularse() {
        actividad.setEstado(EstadoActividad.EN_CURSO);
        RequerimientoActividad requerimiento = requerimiento(7L, 2, 2L);
        prepararBusqueda(
                perfilConProfesion(2L),
                null,
                List.of(requerimiento),
                Map.of(7L, new CupoRequerimiento(2, 0, 2, false)));

        PaginaResponse<ActividadBusquedaResponse> respuesta = buscarActividadService.buscar(
                1L, 100L, null, 0, 20, false);

        assertThat(respuesta.contenido().getFirst().requerimientos()).singleElement()
                .satisfies(r -> assertThat(r.puedePostularse()).isFalse());
    }

    @Test
    void buscar_miembroDirector_noPuedePostularse() {
        RequerimientoActividad requerimiento = requerimiento(7L, 2, 2L);
        prepararBusqueda(
                perfilConProfesion(2L),
                miembro("DIRECTOR"),
                List.of(requerimiento),
                Map.of(7L, new CupoRequerimiento(2, 0, 2, false)));

        PaginaResponse<ActividadBusquedaResponse> respuesta = buscarActividadService.buscar(
                1L, 100L, null, 0, 20, false);

        assertThat(respuesta.contenido().getFirst().requerimientos()).singleElement()
                .satisfies(r -> assertThat(r.puedePostularse()).isFalse());
    }

    @Test
    void buscar_miembroNoDirector_conCupo_puedePostularse() {
        RequerimientoActividad requerimiento = requerimiento(7L, 2, 2L);
        prepararBusqueda(
                perfilConProfesion(2L),
                miembro("MIEMBRO"),
                List.of(requerimiento),
                Map.of(7L, new CupoRequerimiento(2, 0, 2, false)));

        PaginaResponse<ActividadBusquedaResponse> respuesta = buscarActividadService.buscar(
                1L, 100L, null, 0, 20, false);

        assertThat(respuesta.contenido().getFirst().requerimientos()).singleElement()
                .satisfies(r -> assertThat(r.puedePostularse()).isTrue());
    }

    @Test
    void buscar_profesionDistinta_noPuedePostularse() {
        RequerimientoActividad requerimiento = requerimiento(7L, 2, 2L);
        prepararBusqueda(
                perfilConProfesion(3L),
                null,
                List.of(requerimiento),
                Map.of(7L, new CupoRequerimiento(2, 0, 2, false)));

        PaginaResponse<ActividadBusquedaResponse> respuesta = buscarActividadService.buscar(
                1L, 100L, null, 0, 20, false);

        assertThat(respuesta.contenido().getFirst().requerimientos()).singleElement()
                .satisfies(r -> assertThat(r.puedePostularse()).isFalse());
    }

    @Test
    void buscar_proyectoNoHabilitado_noPuedePostularse() {
        proyecto.setEstado(EstadoProyecto.BORRADOR);
        RequerimientoActividad requerimiento = requerimiento(7L, 2, 2L);
        prepararBusqueda(
                perfilConProfesion(2L),
                miembro("MIEMBRO"),
                List.of(requerimiento),
                Map.of(7L, new CupoRequerimiento(2, 0, 2, false)));

        PaginaResponse<ActividadBusquedaResponse> respuesta = buscarActividadService.buscar(
                1L, 100L, null, 0, 20, false);

        assertThat(respuesta.contenido().getFirst().requerimientos()).singleElement()
                .satisfies(r -> assertThat(r.puedePostularse()).isFalse());
    }

    @Test
    void buscar_variosRequerimientos_usaCuposDelMapa() {
        RequerimientoActividad primero = requerimiento(7L, 2, 2L);
        RequerimientoActividad segundo = requerimiento(8L, 1, 2L);
        prepararBusqueda(
                perfilConProfesion(2L),
                null,
                List.of(primero, segundo),
                Map.of(
                        7L, new CupoRequerimiento(2, 1, 1, false),
                        8L, new CupoRequerimiento(1, 0, 1, false)));

        PaginaResponse<ActividadBusquedaResponse> respuesta = buscarActividadService.buscar(
                1L, 100L, null, 0, 20, false);

        assertThat(respuesta.contenido().getFirst().requerimientos()).hasSize(2);
        assertThat(respuesta.contenido().getFirst().requerimientos().get(0))
                .satisfies(r -> {
                    assertThat(r.idRequerimiento()).isEqualTo(7L);
                    assertThat(r.cubiertos()).isEqualTo(1L);
                    assertThat(r.faltantes()).isEqualTo(1);
                    assertThat(r.puedePostularse()).isTrue();
                });
        assertThat(respuesta.contenido().getFirst().requerimientos().get(1))
                .satisfies(r -> {
                    assertThat(r.idRequerimiento()).isEqualTo(8L);
                    assertThat(r.cubiertos()).isZero();
                    assertThat(r.faltantes()).isEqualTo(1);
                    assertThat(r.puedePostularse()).isTrue();
                });
    }

    @Test
    void buscar_sinResultados_devuelvePaginaVacia() {
        when(proyectoRepository.findById(100L)).thenReturn(Optional.of(proyecto));

        Page<Actividad> paginaVacia = new PageImpl<>(
                List.of(), PageRequest.of(0, 20), 0);
        when(actividadRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(paginaVacia);

        PaginaResponse<ActividadBusquedaResponse> respuesta = buscarActividadService.buscar(
                1L, 100L, null, 0, 20, false);

        assertThat(respuesta.contenido()).isEmpty();
        assertThat(respuesta.totalElementos()).isZero();
        verify(requerimientoActividadRepository, never()).findConProfesionByActividadIds(any());
        verify(actividadRepository, never()).findConPredecesoras(any());
        verify(perfilRepository, never()).findByIdConProfesion(any());
    }

    @Test
    void buscar_todos_usaPageableSinPaginar() {
        when(proyectoRepository.findById(100L)).thenReturn(Optional.of(proyecto));

        Page<Actividad> paginaVacia = new PageImpl<>(
                List.of(), Pageable.unpaged(), 0);
        when(actividadRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(paginaVacia);

        buscarActividadService.buscar(1L, 100L, null, 3, 20, true);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(actividadRepository).findAll(any(Specification.class), captor.capture());

        Pageable pageable = captor.getValue();
        assertThat(pageable.isUnpaged()).isTrue();
        assertThat(pageable.getSort().getOrderFor("fechaHoraInicio")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("fechaHoraInicio").isAscending()).isTrue();
        assertThat(pageable.getSort().getOrderFor("idActividad")).isNotNull();
    }

    private Perfil perfilConProfesion(Long idProfesion) {
        return Perfil.builder()
                .idPerfil(1L)
                .profesion(Profesion.builder()
                        .idProfesion(idProfesion)
                        .nombre("Modelo")
                        .build())
                .build();
    }

    private MiembroProyecto miembro(String codigoRol) {
        return MiembroProyecto.builder()
                .proyecto(proyecto)
                .rolProyecto(RolProyecto.builder()
                        .codigo(codigoRol)
                        .nombre(codigoRol)
                        .build())
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .build();
    }

    private RequerimientoActividad requerimiento(Long id, int cantidad, Long idProfesion) {
        return RequerimientoActividad.builder()
                .idRequerimiento(id)
                .cantidad(cantidad)
                .descripcion("Requerimiento " + id)
                .profesion(Profesion.builder()
                        .idProfesion(idProfesion)
                        .nombre("Modelo")
                        .build())
                .actividad(actividad)
                .build();
    }

    private void prepararBusqueda(
            Perfil perfil,
            MiembroProyecto membresia,
            List<RequerimientoActividad> requerimientos,
            Map<Long, CupoRequerimiento> cupos) {

        when(proyectoRepository.findById(100L)).thenReturn(Optional.of(proyecto));

        Page<Actividad> paginaActividades = new PageImpl<>(
                List.of(actividad), PageRequest.of(0, 20), 1);
        when(actividadRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(paginaActividades);

        when(perfilRepository.findByIdConProfesion(perfil.getIdPerfil()))
                .thenReturn(Optional.of(perfil));
        when(miembroProyectoRepository.findMiembroActivoConRol(100L, perfil.getIdPerfil()))
                .thenReturn(membresia == null
                        ? Optional.empty()
                        : Optional.of(membresia));
        when(requerimientoActividadRepository.findConProfesionByActividadIds(List.of(10L)))
                .thenReturn(requerimientos);
        when(cupoRequerimientoService.calcularActividades(requerimientos))
                .thenReturn(cupos);
        when(actividadRepository.findConPredecesoras(List.of(10L)))
                .thenReturn(List.of(actividad));
    }
}
