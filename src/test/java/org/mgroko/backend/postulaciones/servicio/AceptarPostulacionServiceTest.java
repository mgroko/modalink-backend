package org.mgroko.backend.postulaciones.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.actividades.dto.CalculoCpm;
import org.mgroko.backend.actividades.dto.RangoFactibleAbsoluto;
import org.mgroko.backend.actividades.servicio.CronogramaService;
import org.mgroko.backend.calendario.dto.ResultadoDisponibilidad;
import org.mgroko.backend.calendario.dto.ResultadoPreferencia;
import org.mgroko.backend.calendario.servicio.CalendarioService;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.PostulacionActividad;
import org.mgroko.backend.modelo.PostulacionGral;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.postulaciones.dto.PostulacionGestionadaResponse;
import org.mgroko.backend.postulaciones.exception.PostulacionNoEncontradaException;
import org.mgroko.backend.postulaciones.exception.PostulacionNoPermitidaException;
import org.mgroko.backend.postulaciones.exception.PostulacionYaGestionadaException;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.proyectos.servicio.CupoRequerimientoService;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PostulacionActividadRepository;
import org.mgroko.backend.repositorio.PostulacionGralRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository;
import org.mgroko.backend.repositorio.RolProyectoRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AceptarPostulacionServiceTest {

    @Mock
    private PerfilRepository perfilRepository;
    @Mock
    private ProyectoRepository proyectoRepository;
    @Mock
    private PostulacionGralRepository postulacionGralRepository;
    @Mock
    private PostulacionActividadRepository postulacionActividadRepository;
    @Mock
    private RequerimientoGralProyectoRepository requerimientoGralProyectoRepository;
    @Mock
    private RequerimientoActividadRepository requerimientoActividadRepository;
    @Mock
    private MiembroProyectoRepository miembroProyectoRepository;
    @Mock
    private AsignacionActividadRepository asignacionActividadRepository;
    @Mock
    private RolProyectoRepository rolProyectoRepository;
    @Mock
    private CupoRequerimientoService cupoRequerimientoService;
    @Mock
    private ProyectoSecurityService proyectoSecurityService;
    @Mock
    private NotificacionService notificacionService;
    @Mock
    private CalendarioService calendarioService;
    @Mock
    private ActividadRepository actividadRepository;
    @Mock
    private CronogramaService cronogramaService;

    private AceptarPostulacionService service;

    private Clock reloj;

    private Perfil director;
    private Perfil postulante;
    private Proyecto proyecto;
    private RequerimientoGralProyecto requerimiento;
    private PostulacionGral postulacion;
    private RolProyecto rolMiembro;

    @BeforeEach
    void setUp() {
        reloj = Clock.fixed(
                Instant.parse("2026-10-27T13:00:00Z"),
                ZoneId.of("America/Argentina/Buenos_Aires"));

        service = new AceptarPostulacionService(
                perfilRepository,
                proyectoRepository,
                postulacionGralRepository,
                postulacionActividadRepository,
                requerimientoGralProyectoRepository,
                requerimientoActividadRepository,
                miembroProyectoRepository,
                asignacionActividadRepository,
                rolProyectoRepository,
                cupoRequerimientoService,
                proyectoSecurityService,
                notificacionService,
                calendarioService,
                actividadRepository,
                cronogramaService,
                reloj);

        director = Perfil.builder()
                .idPerfil(10L)
                .nombreArtistico("Directora")
                .estado(EstadoPerfil.ACTIVO)
                .usuario(Usuario.builder().idUsuario(1L).build())
                .build();

        postulante = Perfil.builder()
                .idPerfil(12L)
                .nombreArtistico("Postulante")
                .estado(EstadoPerfil.ACTIVO)
                .usuario(Usuario.builder().idUsuario(2L).build())
                .profesion(Profesion.builder().idProfesion(2L).nombre("Modelo").build())
                .build();

        proyecto = Proyecto.builder()
                .idProyecto(5L)
                .nombre("Desfile")
                .estado(EstadoProyecto.PUBLICADO)
                .build();

        requerimiento = RequerimientoGralProyecto.builder()
                .idRequerimientoGral(20L)
                .proyecto(proyecto)
                .profesion(postulante.getProfesion())
                .cantidad(1)
                .descripcion("Una modelo")
                .build();

        postulacion = PostulacionGral.builder()
                .idPostulacionGral(77L)
                .perfil(postulante)
                .requerimientoGral(requerimiento)
                .estado(EstadoSolicitud.PENDIENTE)
                .fechaPostulacion(LocalDateTime.of(2026, 10, 26, 10, 0))
                .build();

        rolMiembro = RolProyecto.builder()
                .idRolProyecto(2L)
                .codigo("MIEMBRO")
                .nombre("Miembro de proyecto")
                .build();
    }

    @Test
    void aceptarGeneral_creaMiembroCambiaEstadoYNotifica() {
        prepararCaminoFeliz();

        when(miembroProyectoRepository.saveAndFlush(any(MiembroProyecto.class)))
                .thenAnswer(invocation -> {
                    MiembroProyecto miembro = invocation.getArgument(0);
                    miembro.setIdMiembro(88L);
                    return miembro;
                });
        when(postulacionGralRepository.saveAndFlush(any(PostulacionGral.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(notificacionService.crear(
                any(Usuario.class), any(Perfil.class),
                eq(NotificacionService.TIPO_POSTULACION_ACEPTADA),
                any(String.class), eq("POSTULACION_GRAL"), eq(77L)))
                .thenReturn(new Notificacion());

        PostulacionGestionadaResponse respuesta = service.aceptarGeneral(1L, 10L, 5L, 77L);

        assertThat(respuesta.idPostulacion()).isEqualTo(77L);
        assertThat(respuesta.estado()).isEqualTo(EstadoSolicitud.ACEPTADA);
        assertThat(respuesta.idMiembro()).isEqualTo(88L);
        assertThat(postulacion.getEstado()).isEqualTo(EstadoSolicitud.ACEPTADA);
        assertThat(requerimiento.getIdRequerimientoGral()).isEqualTo(20L);
        verify(miembroProyectoRepository).saveAndFlush(any(MiembroProyecto.class));
        verify(postulacionGralRepository).saveAndFlush(postulacion);
        verify(notificacionService).registrarTodas(any());
    }

    @Test
    void aceptarGeneral_postulacionDeOtroProyecto_lanzaNoEncontrada() {
        prepararCaminoFeliz();
        Proyecto otro = Proyecto.builder().idProyecto(99L).nombre("Otro").build();
        postulacion.getRequerimientoGral().setProyecto(otro);

        assertThatThrownBy(() -> service.aceptarGeneral(1L, 10L, 5L, 77L))
                .isInstanceOf(PostulacionNoEncontradaException.class);

        verify(miembroProyectoRepository, never()).saveAndFlush(any());
    }

    @Test
    void aceptarGeneral_yaGestionada_lanzaExcepcion() {
        prepararCaminoFeliz();
        postulacion.setEstado(EstadoSolicitud.ACEPTADA);

        assertThatThrownBy(() -> service.aceptarGeneral(1L, 10L, 5L, 77L))
                .isInstanceOf(PostulacionYaGestionadaException.class);
    }

    @Test
    void aceptarGeneral_sinCupo_lanzaExcepcion() {
        prepararCaminoFeliz();
        when(cupoRequerimientoService.calcularGeneral(requerimiento))
                .thenReturn(new CupoRequerimiento(1, 1, 0, true));

        assertThatThrownBy(() -> service.aceptarGeneral(1L, 10L, 5L, 77L))
                .isInstanceOf(PostulacionNoPermitidaException.class)
                .hasMessageContaining("satisfecho");

        verify(miembroProyectoRepository, never()).saveAndFlush(any());
    }

    @Test
    void aceptarGeneral_postulanteYaEsMiembro_lanzaExcepcion() {
        prepararCaminoFeliz();
        when(miembroProyectoRepository
                .existsByProyectoIdProyectoAndPerfilIdPerfilAndEstadoParticipacion(
                        5L, 12L, EstadoParticipacion.ACTIVO))
                .thenReturn(true);

        assertThatThrownBy(() -> service.aceptarGeneral(1L, 10L, 5L, 77L))
                .isInstanceOf(PostulacionNoPermitidaException.class)
                .hasMessageContaining("miembro activo");
    }

    @Test
    void aceptarGeneral_sinPerfilActivo_lanzaExcepcion() {
        assertThatThrownBy(() -> service.aceptarGeneral(1L, null, 5L, 77L))
                .isInstanceOf(PerfilActivoNoSeleccionadoException.class);
    }

    @Test
    void aceptarActividad_evaluaAsignadosYDevuelveDesvioYAdvertencia() {
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 1, 9, 0);
        Planificacion planificacion = Planificacion.builder()
                .idPlanificacion(50L)
                .proyecto(proyecto)
                .fechaEntrega(LocalDate.of(2026, 11, 30))
                .build();
        Actividad actividad = Actividad.builder()
                .idActividad(55L)
                .nombre("J")
                .duracionMinutos(60)
                .estado(EstadoActividad.PENDIENTE)
                .planificacion(planificacion)
                .build();
        RequerimientoActividad requerimientoActividad = RequerimientoActividad.builder()
                .idRequerimiento(20L)
                .cantidad(1)
                .profesion(postulante.getProfesion())
                .actividad(actividad)
                .build();
        PostulacionActividad postulacionActividad = PostulacionActividad.builder()
                .idPostulacion(77L)
                .perfil(postulante)
                .requerimiento(requerimientoActividad)
                .estado(EstadoSolicitud.PENDIENTE)
                .build();

        Perfil otroPerfil = Perfil.builder()
                .idPerfil(13L)
                .estado(EstadoPerfil.ACTIVO)
                .usuario(Usuario.builder().idUsuario(3L).build())
                .build();
        MiembroProyecto otroMiembro = MiembroProyecto.builder()
                .idMiembro(80L)
                .perfil(otroPerfil)
                .proyecto(proyecto)
                .build();
        AsignacionActividad otraAsignacion = AsignacionActividad.builder()
                .idAsignacionAct(90L)
                .miembro(otroMiembro)
                .actividad(actividad)
                .requerimientoActividad(requerimientoActividad)
                .build();

        Proyecto otroProyecto = Proyecto.builder()
                .idProyecto(6L)
                .nombre("Otro proyecto")
                .estado(EstadoProyecto.PUBLICADO)
                .build();
        Planificacion otraPlanificacion = Planificacion.builder()
                .idPlanificacion(60L)
                .proyecto(otroProyecto)
                .fechaEntrega(LocalDate.of(2026, 11, 30))
                .build();
        Actividad otraActividad = Actividad.builder()
                .idActividad(66L)
                .nombre("Otra")
                .duracionMinutos(60)
                .estado(EstadoActividad.PENDIENTE)
                .planificacion(otraPlanificacion)
                .build();
        AsignacionActividad otraSinHorario = AsignacionActividad.builder()
                .idAsignacionAct(91L)
                .miembro(MiembroProyecto.builder()
                        .idMiembro(81L)
                        .perfil(postulante)
                        .proyecto(otroProyecto)
                        .build())
                .actividad(otraActividad)
                .build();

        MiembroProyecto miembroDirector = MiembroProyecto.builder()
                .idMiembro(82L)
                .perfil(director)
                .proyecto(proyecto)
                .build();

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L))
                .thenReturn(Optional.of(director));
        when(proyectoRepository.findByIdParaGestion(5L))
                .thenReturn(Optional.of(proyecto));
        when(postulacionActividadRepository.findByIdParaGestion(77L))
                .thenReturn(Optional.of(postulacionActividad));
        when(requerimientoActividadRepository.findByIdParaGestion(20L))
                .thenReturn(Optional.of(requerimientoActividad));
        when(cupoRequerimientoService.calcularActividad(requerimientoActividad))
                .thenReturn(new CupoRequerimiento(1, 0, 1, false));
        when(miembroProyectoRepository.findMiembroActivoConRol(5L, 12L))
                .thenReturn(Optional.empty());
        when(asignacionActividadRepository.findActivasByActividadId(55L))
                .thenReturn(List.of(otraAsignacion));
        when(calendarioService.verificarDisponibilidadMiembros(
                anyCollection(), eq(actividad)))
                .thenReturn(new ResultadoDisponibilidad(
                        true,
                        false,
                        inicio,
                        inicio.plusHours(9),
                        inicio.plusMinutes(30),
                        inicio.plusMinutes(90),
                        null,
                        List.of(),
                        ResultadoPreferencia.DESVIO,
                        30L,
                        inicio.plusMinutes(30),
                        List.of(3L)));
        when(asignacionActividadRepository.findActivasSinHorarioDeUsuarioEnOtrosProyectos(
                2L, 5L, 55L)).thenReturn(List.of(otraSinHorario));
        when(actividadRepository.findConPredecesorasByPlanificacion(60L))
                .thenReturn(List.of(otraActividad));
        when(cronogramaService.calcularProyeccion(eq(otraPlanificacion), any()))
                .thenReturn(new CalculoCpm(Map.of(), 60L, 1000L, true, List.of()));
        when(cronogramaService.rangoFactibleAbsoluto(
                eq(otraPlanificacion), any(), eq(66L)))
                .thenReturn(new RangoFactibleAbsoluto(
                        inicio,
                        inicio.plusHours(9),
                        inicio.plusHours(1),
                        inicio.plusHours(10),
                        false,
                        false,
                        false,
                        null));
        when(rolProyectoRepository.findByCodigo("MIEMBRO"))
                .thenReturn(Optional.of(rolMiembro));
        when(miembroProyectoRepository.saveAndFlush(any(MiembroProyecto.class)))
                .thenAnswer(invocation -> {
                    MiembroProyecto miembro = invocation.getArgument(0);
                    miembro.setIdMiembro(900L);
                    return miembro;
                });
        when(asignacionActividadRepository.saveAndFlush(any(AsignacionActividad.class)))
                .thenAnswer(invocation -> {
                    AsignacionActividad asignacion = invocation.getArgument(0);
                    asignacion.setIdAsignacionAct(1200L);
                    return asignacion;
                });
        when(postulacionActividadRepository.saveAndFlush(any(PostulacionActividad.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(notificacionService.crear(
                any(Usuario.class), any(Perfil.class), any(String.class),
                any(String.class), any(String.class), any(Long.class)))
                .thenReturn(new Notificacion());
        when(miembroProyectoRepository.buscarDirectoresDeProyectos(List.of(5L)))
                .thenReturn(List.of(miembroDirector));

        PostulacionGestionadaResponse respuesta = service.aceptarActividad(1L, 10L, 5L, 77L);

        assertThat(respuesta.estado()).isEqualTo(EstadoSolicitud.ACEPTADA);
        assertThat(respuesta.resultadoPreferencia()).isEqualTo(ResultadoPreferencia.DESVIO);
        assertThat(respuesta.desvioMinutos()).isEqualTo(30L);
        assertThat(respuesta.advertencias()).singleElement().satisfies(advertencia -> {
            assertThat(advertencia.idActividad()).isEqualTo(66L);
            assertThat(advertencia.nombreProyecto()).isEqualTo("Otro proyecto");
        });
        verify(calendarioService).verificarDisponibilidadMiembros(
                org.mockito.ArgumentMatchers.argThat(ids -> ids.size() == 2),
                eq(actividad));
    }

    private void prepararCaminoFeliz() {
        lenient().when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L))
                .thenReturn(Optional.of(director));
        lenient().when(proyectoRepository.findByIdParaGestion(5L))
                .thenReturn(Optional.of(proyecto));
        lenient().when(postulacionGralRepository.findByIdParaGestion(77L))
                .thenReturn(Optional.of(postulacion));
        lenient().when(requerimientoGralProyectoRepository.findByIdParaGestion(20L))
                .thenReturn(Optional.of(requerimiento));
        lenient().when(cupoRequerimientoService.calcularGeneral(requerimiento))
                .thenReturn(new CupoRequerimiento(1, 0, 1, false));
        lenient().when(miembroProyectoRepository
                .existsByProyectoIdProyectoAndPerfilIdPerfilAndEstadoParticipacion(
                        5L, 12L, EstadoParticipacion.ACTIVO))
                .thenReturn(false);
        lenient().when(rolProyectoRepository.findByCodigo("MIEMBRO"))
                .thenReturn(Optional.of(rolMiembro));
    }
}
