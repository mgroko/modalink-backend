package org.mgroko.backend.postulaciones.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.PostulacionActividad;
import org.mgroko.backend.modelo.PostulacionGral;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.postulaciones.dto.BuscarPostulacionesFiltro;
import org.mgroko.backend.postulaciones.dto.PostulacionBusquedaResponse;
import org.mgroko.backend.postulaciones.dto.TipoPostulacion;
import org.mgroko.backend.postulaciones.exception.RangoFechasPostulacionInvalidoException;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.proyectos.exception.ProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.servicio.CupoRequerimientoService;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PostulacionActividadRepository;
import org.mgroko.backend.repositorio.PostulacionGralRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.springframework.data.jpa.domain.Specification;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BuscarPostulacionesProyectoServiceTest {

    @Mock
    private ProyectoRepository proyectoRepository;

    @Mock
    private PostulacionGralRepository postulacionGralRepository;

    @Mock
    private PostulacionActividadRepository postulacionActividadRepository;

    @Mock
    private MiembroProyectoRepository miembroProyectoRepository;

    @Mock
    private AsignacionActividadRepository asignacionActividadRepository;

    @Mock
    private CupoRequerimientoService cupoRequerimientoService;

    @Mock
    private ProyectoSecurityService proyectoSecurityService;

    @InjectMocks
    private BuscarPostulacionesProyectoService service;

    private static final Long ID_PROYECTO = 5L;

    @Test
    void buscar_ordenaPendientesPrimeroYUneAmbosTipos() {
        Proyecto proyecto = proyecto();
        Perfil perfil = perfil(12L, "Luna", 2L);
        RequerimientoGralProyecto requerimientoGral = requerimientoGral(proyecto, 2);
        Actividad actividad = actividad(proyecto, EstadoActividad.PENDIENTE);
        RequerimientoActividad requerimientoActividad = requerimientoActividad(actividad, 1);

        PostulacionGral generalAceptada = postulacionGral(
                1L, perfil, requerimientoGral, EstadoSolicitud.ACEPTADA,
                LocalDateTime.of(2026, 10, 25, 10, 0));
        PostulacionGral generalPendiente = postulacionGral(
                2L, perfil, requerimientoGral, EstadoSolicitud.PENDIENTE,
                LocalDateTime.of(2026, 10, 24, 10, 0));
        PostulacionActividad actividadPendiente = postulacionActividad(
                3L, perfil, requerimientoActividad, EstadoSolicitud.PENDIENTE,
                LocalDateTime.of(2026, 10, 26, 10, 0));

        prepararProyecto(proyecto);
        when(miembroProyectoRepository.buscarDirectoresDeProyectos(List.of(ID_PROYECTO)))
                .thenReturn(List.of());
        when(asignacionActividadRepository.findActivasDeProyectoConRequerimiento(ID_PROYECTO))
                .thenReturn(List.of());
        when(postulacionGralRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(generalAceptada, generalPendiente));
        when(postulacionActividadRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(actividadPendiente));
        when(cupoRequerimientoService.calcularGeneral(requerimientoGral))
                .thenReturn(new CupoRequerimiento(2, 1, 1, false));
        when(cupoRequerimientoService.calcularActividad(requerimientoActividad))
                .thenReturn(new CupoRequerimiento(1, 0, 1, false));

        PaginaResponse<PostulacionBusquedaResponse> pagina = service.buscar(
                10L, ID_PROYECTO, filtro(null), 0, 20, false);

        assertThat(pagina.contenido()).extracting(PostulacionBusquedaResponse::estado)
                .containsExactly(
                        EstadoSolicitud.PENDIENTE,
                        EstadoSolicitud.PENDIENTE,
                        EstadoSolicitud.ACEPTADA);
        assertThat(pagina.contenido()).extracting(PostulacionBusquedaResponse::tipo)
                .containsExactly(
                        TipoPostulacion.ACTIVIDAD,
                        TipoPostulacion.GENERAL,
                        TipoPostulacion.GENERAL);
        assertThat(pagina.totalElementos()).isEqualTo(3);
    }

    @Test
    void buscar_tipoGeneral_noConsultaActividades() {
        Proyecto proyecto = proyecto();
        Perfil perfil = perfil(12L, "Luna", 2L);
        RequerimientoGralProyecto requerimientoGral = requerimientoGral(proyecto, 2);
        PostulacionGral postulacion = postulacionGral(
                1L, perfil, requerimientoGral, EstadoSolicitud.PENDIENTE,
                LocalDateTime.of(2026, 10, 25, 10, 0));

        prepararProyecto(proyecto);
        when(postulacionGralRepository.findAll(any(Specification.class))).thenReturn(List.of(postulacion));
        when(cupoRequerimientoService.calcularGeneral(requerimientoGral))
                .thenReturn(new CupoRequerimiento(2, 0, 2, false));

        PaginaResponse<PostulacionBusquedaResponse> pagina = service.buscar(
                10L, ID_PROYECTO,
                new BuscarPostulacionesFiltro(null, TipoPostulacion.GENERAL, null, null, null, null),
                0, 20, false);

        assertThat(pagina.contenido()).hasSize(1);
        verify(postulacionActividadRepository, never()).findAll(any(Specification.class));
    }

    @Test
    void buscar_pendienteSinCupoNoPuedeAceptarse() {
        Proyecto proyecto = proyecto();
        Perfil perfil = perfil(12L, "Luna", 2L);
        RequerimientoGralProyecto requerimiento = requerimientoGral(proyecto, 1);
        PostulacionGral postulacion = postulacionGral(
                1L, perfil, requerimiento, EstadoSolicitud.PENDIENTE,
                LocalDateTime.of(2026, 10, 25, 10, 0));

        prepararProyecto(proyecto);
        when(postulacionGralRepository.findAll(any(Specification.class))).thenReturn(List.of(postulacion));
        when(cupoRequerimientoService.calcularGeneral(requerimiento))
                .thenReturn(new CupoRequerimiento(1, 1, 0, true));

        PostulacionBusquedaResponse respuesta = service.buscar(
                10L, ID_PROYECTO, filtro(null), 0, 20, false).contenido().get(0);

        assertThat(respuesta.satisfecho()).isTrue();
        assertThat(respuesta.puedeAceptar()).isFalse();
        assertThat(respuesta.puedeRechazar()).isTrue();
    }

    @Test
    void buscar_sinPerfilActivo_lanzaExcepcion() {
        assertThatThrownBy(() -> service.buscar(
                null, ID_PROYECTO, filtro(null), 0, 20, false))
                .isInstanceOf(PerfilActivoNoSeleccionadoException.class);
    }

    @Test
    void buscar_proyectoInexistente_lanzaExcepcion() {
        when(proyectoRepository.findById(ID_PROYECTO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(
                10L, ID_PROYECTO, filtro(null), 0, 20, false))
                .isInstanceOf(ProyectoNoEncontradoException.class);
    }

    @Test
    void buscar_rangoDeFechasInvalido_lanzaExcepcion() {
        BuscarPostulacionesFiltro filtro = new BuscarPostulacionesFiltro(
                null, null, null, null,
                LocalDate.of(2026, 10, 30), LocalDate.of(2026, 10, 1));

        assertThatThrownBy(() -> service.buscar(10L, ID_PROYECTO, filtro, 0, 20, false))
                .isInstanceOf(RangoFechasPostulacionInvalidoException.class);
    }

    private void prepararProyecto(Proyecto proyecto) {
        when(proyectoRepository.findById(ID_PROYECTO)).thenReturn(Optional.of(proyecto));
        when(miembroProyectoRepository.buscarMiembrosActivosDeProyecto(ID_PROYECTO))
                .thenReturn(List.of());
    }

    private BuscarPostulacionesFiltro filtro(TipoPostulacion tipo) {
        return new BuscarPostulacionesFiltro(null, tipo, null, null, null, null);
    }

    private Proyecto proyecto() {
        return Proyecto.builder()
                .idProyecto(ID_PROYECTO)
                .nombre("Proyecto UC-74")
                .estado(EstadoProyecto.PUBLICADO)
                .build();
    }

    private Perfil perfil(Long idPerfil, String nombre, Long idProfesion) {
        return Perfil.builder()
                .idPerfil(idPerfil)
                .nombreArtistico(nombre)
                .profesion(Profesion.builder()
                        .idProfesion(idProfesion)
                        .nombre("Modelo")
                        .build())
                .build();
    }

    private RequerimientoGralProyecto requerimientoGral(Proyecto proyecto, int cantidad) {
        return RequerimientoGralProyecto.builder()
                .idRequerimientoGral(20L)
                .proyecto(proyecto)
                .cantidad(cantidad)
                .descripcion("Dos modelos")
                .build();
    }

    private PostulacionGral postulacionGral(
            Long id, Perfil perfil, RequerimientoGralProyecto requerimiento,
            EstadoSolicitud estado, LocalDateTime fecha) {
        return PostulacionGral.builder()
                .idPostulacionGral(id)
                .perfil(perfil)
                .requerimientoGral(requerimiento)
                .estado(estado)
                .fechaPostulacion(fecha)
                .build();
    }

    private Planificacion planificacion(Proyecto proyecto) {
        return Planificacion.builder()
                .idPlanificacion(30L)
                .proyecto(proyecto)
                .build();
    }

    private Actividad actividad(Proyecto proyecto, EstadoActividad estado) {
        return Actividad.builder()
                .idActividad(40L)
                .nombre("Prueba de vestuario")
                .estado(estado)
                .duracionMinutos(120)
                .planificacion(planificacion(proyecto))
                .build();
    }

    private RequerimientoActividad requerimientoActividad(Actividad actividad, int cantidad) {
        return RequerimientoActividad.builder()
                .idRequerimiento(50L)
                .actividad(actividad)
                .cantidad(cantidad)
                .descripcion("Una modelo")
                .build();
    }

    private PostulacionActividad postulacionActividad(
            Long id, Perfil perfil, RequerimientoActividad requerimiento,
            EstadoSolicitud estado, LocalDateTime fecha) {
        return PostulacionActividad.builder()
                .idPostulacion(id)
                .perfil(perfil)
                .requerimiento(requerimiento)
                .estado(estado)
                .fechaPostulacion(fecha)
                .build();
    }
}
