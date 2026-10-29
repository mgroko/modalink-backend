package org.mgroko.backend.postulaciones.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.calendario.dto.ResultadoDisponibilidad;
import org.mgroko.backend.calendario.servicio.CalendarioService;
import org.mgroko.backend.modelo.Actividad;
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
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.postulaciones.dto.PostulacionResponse;
import org.mgroko.backend.postulaciones.dto.TipoPostulacion;
import org.mgroko.backend.postulaciones.exception.PostulacionNoPermitidaException;
import org.mgroko.backend.postulaciones.exception.RequerimientoNoEncontradoException;
import org.mgroko.backend.proyectos.dto.CupoRequerimiento;
import org.mgroko.backend.proyectos.servicio.CupoRequerimientoService;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PostulacionActividadRepository;
import org.mgroko.backend.repositorio.PostulacionGralRepository;
import org.mgroko.backend.repositorio.RequerimientoActividadRepository;
import org.mgroko.backend.repositorio.RequerimientoGralProyectoRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class CrearPostulacionServiceTest {

    private static final Long ID_USUARIO = 1L;
    private static final Long ID_PERFIL = 10L;
    private static final Long ID_PROYECTO = 5L;
    private static final Long ID_REQUERIMIENTO_ACT = 20L;
    private static final Long ID_REQUERIMIENTO_GRAL = 30L;
    private static final Long ID_PROFESION = 3L;

    private static final Clock RELOJ = Clock.fixed(
            Instant.parse("2026-10-25T15:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 25, 15, 0);

    @Mock
    private PerfilRepository perfilRepository;
    @Mock
    private RequerimientoActividadRepository requerimientoActividadRepository;
    @Mock
    private RequerimientoGralProyectoRepository requerimientoGralProyectoRepository;
    @Mock
    private PostulacionActividadRepository postulacionActividadRepository;
    @Mock
    private PostulacionGralRepository postulacionGralRepository;
    @Mock
    private MiembroProyectoRepository miembroProyectoRepository;
    @Mock
    private CupoRequerimientoService cupoRequerimientoService;
    @Mock
    private NotificacionService notificacionService;
    @Mock
    private CalendarioService calendarioService;

    private CrearPostulacionService servicio;

    @BeforeEach
    void setUp() {
        servicio = new CrearPostulacionService(
                perfilRepository,
                requerimientoActividadRepository,
                requerimientoGralProyectoRepository,
                postulacionActividadRepository,
                postulacionGralRepository,
                miembroProyectoRepository,
                cupoRequerimientoService,
                notificacionService,
                calendarioService,
                RELOJ);
    }

    // ---------------------------------------------------------------
    // Postulación a requerimiento general
    // ---------------------------------------------------------------

    @Test
    void general_conPerfilApto_persisteYNotificaADirectores() {
        Perfil perfil = perfil(EstadoPerfil.ACTIVO, ID_PROFESION);
        RequerimientoGralProyecto requerimiento = requerimientoGral(proyectoPublicado(true));

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL, ID_USUARIO))
                .thenReturn(Optional.of(perfil));
        when(requerimientoGralProyectoRepository.findByIdConProyecto(ID_REQUERIMIENTO_GRAL))
                .thenReturn(Optional.of(requerimiento));
        when(cupoRequerimientoService.calcularGeneral(requerimiento))
                .thenReturn(new CupoRequerimiento(1, 0, 1, false));
        when(postulacionGralRepository
                .existsByPerfil_IdPerfilAndRequerimientoGral_IdRequerimientoGral(
                        ID_PERFIL, ID_REQUERIMIENTO_GRAL)).thenReturn(false);
        when(postulacionGralRepository.saveAndFlush(any(PostulacionGral.class)))
                .thenAnswer(invocacion -> {
                    PostulacionGral p = invocacion.getArgument(0);
                    p.setIdPostulacionGral(77L);
                    return p;
                });
        when(miembroProyectoRepository.buscarDirectoresDeProyectos(List.of(ID_PROYECTO)))
                .thenReturn(List.of(director(50L), director(51L)));
        when(notificacionService.crear(
                any(), any(), anyString(), anyString(), anyString(), any(), any()))
                .thenAnswer(invocacion -> Notificacion.builder()
                        .usuarioDestino(invocacion.getArgument(0))
                        .perfilContexto(invocacion.getArgument(1))
                        .tipo(invocacion.getArgument(2))
                        .mensaje(invocacion.getArgument(3))
                        .entidadTipo(invocacion.getArgument(4))
                        .entidadId(invocacion.getArgument(5))
                        .idProyecto(invocacion.getArgument(6))
                        .build());

        PostulacionResponse respuesta = servicio.darDeAltaPostulacionGeneral(
                ID_USUARIO, ID_PERFIL, ID_PROYECTO, ID_REQUERIMIENTO_GRAL);

        assertThat(respuesta.idPostulacion()).isEqualTo(77L);
        assertThat(respuesta.tipo()).isEqualTo(TipoPostulacion.GENERAL);
        assertThat(respuesta.estado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(respuesta.fechaPostulacion()).isEqualTo(AHORA);
        assertThat(respuesta.idProyecto()).isEqualTo(ID_PROYECTO);
        assertThat(respuesta.idRequerimiento()).isEqualTo(ID_REQUERIMIENTO_GRAL);
        assertThat(respuesta.tentativa()).isFalse();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Notificacion>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificacionService).registrarTodas(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue())
                .allSatisfy(n -> assertThat(n.getTipo())
                        .isEqualTo(NotificacionService.TIPO_POSTULACION_RECIBIDA));
        assertThat(captor.getValue())
                .allSatisfy(n -> assertThat(n.getEntidadTipo())
                        .isEqualTo(CrearPostulacionService.ENTIDAD_POSTULACION_GRAL));
        assertThat(captor.getValue())
                .allSatisfy(n -> assertThat(n.getEntidadId()).isEqualTo(77L));
        assertThat(captor.getValue())
                .allSatisfy(n -> assertThat(n.getIdProyecto()).isEqualTo(ID_PROYECTO));
    }

    @Test
    void general_proyectoNoPublicado_lanzaExcepcion() {
        RequerimientoGralProyecto requerimiento = requerimientoGral(proyecto(EstadoProyecto.BORRADOR, true));

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL, ID_USUARIO))
                .thenReturn(Optional.of(perfil(EstadoPerfil.ACTIVO, ID_PROFESION)));
        when(requerimientoGralProyectoRepository.findByIdConProyecto(ID_REQUERIMIENTO_GRAL))
                .thenReturn(Optional.of(requerimiento));

        assertThatThrownBy(() -> servicio.darDeAltaPostulacionGeneral(
                ID_USUARIO, ID_PERFIL, ID_PROYECTO, ID_REQUERIMIENTO_GRAL))
                .isInstanceOf(PostulacionNoPermitidaException.class);

        verify(postulacionGralRepository, never()).saveAndFlush(any());
    }

    @Test
    void general_proyectoNoAceptaPostulacionesGenerales_lanzaExcepcion() {
        RequerimientoGralProyecto requerimiento = requerimientoGral(proyectoPublicado(false));

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL, ID_USUARIO))
                .thenReturn(Optional.of(perfil(EstadoPerfil.ACTIVO, ID_PROFESION)));
        when(requerimientoGralProyectoRepository.findByIdConProyecto(ID_REQUERIMIENTO_GRAL))
                .thenReturn(Optional.of(requerimiento));

        assertThatThrownBy(() -> servicio.darDeAltaPostulacionGeneral(
                ID_USUARIO, ID_PERFIL, ID_PROYECTO, ID_REQUERIMIENTO_GRAL))
                .isInstanceOf(PostulacionNoPermitidaException.class);
    }

    @Test
    void general_requerimientoDeOtroProyecto_lanzaNoEncontrado() {
        RequerimientoGralProyecto requerimiento = requerimientoGral(proyectoPublicado(true));

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL, ID_USUARIO))
                .thenReturn(Optional.of(perfil(EstadoPerfil.ACTIVO, ID_PROFESION)));
        when(requerimientoGralProyectoRepository.findByIdConProyecto(ID_REQUERIMIENTO_GRAL))
                .thenReturn(Optional.of(requerimiento));

        assertThatThrownBy(() -> servicio.darDeAltaPostulacionGeneral(
                ID_USUARIO, ID_PERFIL, 99L, ID_REQUERIMIENTO_GRAL))
                .isInstanceOf(RequerimientoNoEncontradoException.class);
    }

    @Test
    void general_requerimientoInexistente_lanzaNoEncontrado() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL, ID_USUARIO))
                .thenReturn(Optional.of(perfil(EstadoPerfil.ACTIVO, ID_PROFESION)));
        when(requerimientoGralProyectoRepository.findByIdConProyecto(ID_REQUERIMIENTO_GRAL))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.darDeAltaPostulacionGeneral(
                ID_USUARIO, ID_PERFIL, ID_PROYECTO, ID_REQUERIMIENTO_GRAL))
                .isInstanceOf(RequerimientoNoEncontradoException.class);
    }

    @Test
    void general_sinPerfilActivo_lanzaPerfilActivoNoSeleccionado() {
        assertThatThrownBy(() -> servicio.darDeAltaPostulacionGeneral(
                ID_USUARIO, null, ID_PROYECTO, ID_REQUERIMIENTO_GRAL))
                .isInstanceOf(PerfilActivoNoSeleccionadoException.class);
    }

    @Test
    void general_perfilDeOtroUsuario_lanzaPerfilNoEncontrado() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL, ID_USUARIO))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.darDeAltaPostulacionGeneral(
                ID_USUARIO, ID_PERFIL, ID_PROYECTO, ID_REQUERIMIENTO_GRAL))
                .isInstanceOf(PerfilNoEncontradoException.class);
    }

    @Test
    void general_perfilInactivo_lanzaPerfilEnBaja() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL, ID_USUARIO))
                .thenReturn(Optional.of(perfil(EstadoPerfil.BAJA, ID_PROFESION)));

        assertThatThrownBy(() -> servicio.darDeAltaPostulacionGeneral(
                ID_USUARIO, ID_PERFIL, ID_PROYECTO, ID_REQUERIMIENTO_GRAL))
                .isInstanceOf(PerfilEnBajaException.class);
    }

    @Test
    void general_perfilYaEsMiembroActivo_lanzaExcepcion() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL, ID_USUARIO))
                .thenReturn(Optional.of(perfil(EstadoPerfil.ACTIVO, ID_PROFESION)));
        when(requerimientoGralProyectoRepository.findByIdConProyecto(ID_REQUERIMIENTO_GRAL))
                .thenReturn(Optional.of(requerimientoGral(proyectoPublicado(true))));
        when(miembroProyectoRepository.findMiembroActivoConRol(ID_PROYECTO, ID_PERFIL))
                .thenReturn(Optional.of(miembro("MIEMBRO")));

        assertThatThrownBy(() -> servicio.darDeAltaPostulacionGeneral(
                ID_USUARIO, ID_PERFIL, ID_PROYECTO, ID_REQUERIMIENTO_GRAL))
                .isInstanceOf(PostulacionNoPermitidaException.class)
                .hasMessageContaining("miembro activo");
    }

    @Test
    void general_profesionDistinta_lanzaExcepcion() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL, ID_USUARIO))
                .thenReturn(Optional.of(perfil(EstadoPerfil.ACTIVO, 99L)));
        when(requerimientoGralProyectoRepository.findByIdConProyecto(ID_REQUERIMIENTO_GRAL))
                .thenReturn(Optional.of(requerimientoGral(proyectoPublicado(true))));

        assertThatThrownBy(() -> servicio.darDeAltaPostulacionGeneral(
                ID_USUARIO, ID_PERFIL, ID_PROYECTO, ID_REQUERIMIENTO_GRAL))
                .isInstanceOf(PostulacionNoPermitidaException.class)
                .hasMessageContaining("profesión");
    }

    @Test
    void general_requerimientoSatisfecho_lanzaExcepcion() {
        RequerimientoGralProyecto requerimiento = requerimientoGral(proyectoPublicado(true));
        requerimiento.setCantidad(2);

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL, ID_USUARIO))
                .thenReturn(Optional.of(perfil(EstadoPerfil.ACTIVO, ID_PROFESION)));
        when(requerimientoGralProyectoRepository.findByIdConProyecto(ID_REQUERIMIENTO_GRAL))
                .thenReturn(Optional.of(requerimiento));
        when(cupoRequerimientoService.calcularGeneral(requerimiento))
                .thenReturn(new CupoRequerimiento(2, 2, 0, true));

        assertThatThrownBy(() -> servicio.darDeAltaPostulacionGeneral(
                ID_USUARIO, ID_PERFIL, ID_PROYECTO, ID_REQUERIMIENTO_GRAL))
                .isInstanceOf(PostulacionNoPermitidaException.class)
                .hasMessageContaining("satisfecho");
    }

    @Test
    void general_postulacionDuplicada_lanzaExcepcion() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL, ID_USUARIO))
                .thenReturn(Optional.of(perfil(EstadoPerfil.ACTIVO, ID_PROFESION)));
        when(requerimientoGralProyectoRepository.findByIdConProyecto(ID_REQUERIMIENTO_GRAL))
                .thenReturn(Optional.of(requerimientoGral(proyectoPublicado(true))));
        when(cupoRequerimientoService.calcularGeneral(any(RequerimientoGralProyecto.class)))
                .thenReturn(new CupoRequerimiento(1, 0, 1, false));
        when(postulacionGralRepository
                .existsByPerfil_IdPerfilAndRequerimientoGral_IdRequerimientoGral(
                        ID_PERFIL, ID_REQUERIMIENTO_GRAL)).thenReturn(true);

        assertThatThrownBy(() -> servicio.darDeAltaPostulacionGeneral(
                ID_USUARIO, ID_PERFIL, ID_PROYECTO, ID_REQUERIMIENTO_GRAL))
                .isInstanceOf(PostulacionNoPermitidaException.class)
                .hasMessageContaining("Ya existe una postulación");
    }

    @Test
    void general_sinDirectores_noRegistraNotificaciones() {
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL, ID_USUARIO))
                .thenReturn(Optional.of(perfil(EstadoPerfil.ACTIVO, ID_PROFESION)));
        when(requerimientoGralProyectoRepository.findByIdConProyecto(ID_REQUERIMIENTO_GRAL))
                .thenReturn(Optional.of(requerimientoGral(proyectoPublicado(true))));
        when(cupoRequerimientoService.calcularGeneral(any(RequerimientoGralProyecto.class)))
                .thenReturn(new CupoRequerimiento(1, 0, 1, false));
        when(postulacionGralRepository.saveAndFlush(any(PostulacionGral.class)))
                .thenAnswer(invocacion -> {
                    PostulacionGral p = invocacion.getArgument(0);
                    p.setIdPostulacionGral(77L);
                    return p;
                });
        when(miembroProyectoRepository.buscarDirectoresDeProyectos(List.of(ID_PROYECTO)))
                .thenReturn(List.of());

        servicio.darDeAltaPostulacionGeneral(
                ID_USUARIO, ID_PERFIL, ID_PROYECTO, ID_REQUERIMIENTO_GRAL);

        verify(notificacionService, never()).registrarTodas(anyCollection());
    }

    // ---------------------------------------------------------------
    // Postulación a requerimiento de actividad
    // ---------------------------------------------------------------

    @Test
    void actividad_conDisponibilidad_persisteYNotifica() {
        RequerimientoActividad requerimiento = requerimientoActividad(EstadoActividad.PENDIENTE);

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL, ID_USUARIO))
                .thenReturn(Optional.of(perfil(EstadoPerfil.ACTIVO, ID_PROFESION)));
        when(requerimientoActividadRepository.findByIdConActividadYProyecto(ID_REQUERIMIENTO_ACT))
                .thenReturn(Optional.of(requerimiento));
        when(cupoRequerimientoService.calcularActividad(requerimiento))
                .thenReturn(new CupoRequerimiento(1, 0, 1, false));
        when(postulacionActividadRepository
                .existsByPerfil_IdPerfilAndRequerimiento_IdRequerimiento(
                        ID_PERFIL, ID_REQUERIMIENTO_ACT)).thenReturn(false);
        when(calendarioService.verificarDisponibilidad(ID_USUARIO, requerimiento.getActividad()))
                .thenReturn(new ResultadoDisponibilidad(true, false, AHORA, AHORA.plusHours(2)));
        when(postulacionActividadRepository.saveAndFlush(any(PostulacionActividad.class)))
                .thenAnswer(invocacion -> {
                    PostulacionActividad p = invocacion.getArgument(0);
                    p.setIdPostulacion(88L);
                    return p;
                });
        when(miembroProyectoRepository.buscarDirectoresDeProyectos(List.of(ID_PROYECTO)))
                .thenReturn(List.of(director(50L)));
        when(notificacionService.crear(
                any(), any(), anyString(), anyString(), anyString(), any(), any()))
                .thenAnswer(invocacion -> Notificacion.builder()
                        .tipo(invocacion.getArgument(2))
                        .build());

        PostulacionResponse respuesta = servicio.darDeAltaPostulacionActividad(
                ID_USUARIO, ID_PERFIL, ID_PROYECTO, ID_REQUERIMIENTO_ACT);

        assertThat(respuesta.idPostulacion()).isEqualTo(88L);
        assertThat(respuesta.tipo()).isEqualTo(TipoPostulacion.ACTIVIDAD);
        assertThat(respuesta.estado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(respuesta.fechaPostulacion()).isEqualTo(AHORA);
        assertThat(respuesta.idRequerimiento()).isEqualTo(ID_REQUERIMIENTO_ACT);
        assertThat(respuesta.tentativa()).isFalse();

        verify(notificacionService).registrarTodas(anyCollection());
    }

    @Test
    void actividad_sinDisponibilidad_lanzaExcepcionYNoPersiste() {
        RequerimientoActividad requerimiento = requerimientoActividad(EstadoActividad.PENDIENTE);

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL, ID_USUARIO))
                .thenReturn(Optional.of(perfil(EstadoPerfil.ACTIVO, ID_PROFESION)));
        when(requerimientoActividadRepository.findByIdConActividadYProyecto(ID_REQUERIMIENTO_ACT))
                .thenReturn(Optional.of(requerimiento));
        when(cupoRequerimientoService.calcularActividad(requerimiento))
                .thenReturn(new CupoRequerimiento(1, 0, 1, false));
        when(calendarioService.verificarDisponibilidad(ID_USUARIO, requerimiento.getActividad()))
                .thenReturn(new ResultadoDisponibilidad(false, false, AHORA, AHORA.plusHours(2)));

        assertThatThrownBy(() -> servicio.darDeAltaPostulacionActividad(
                ID_USUARIO, ID_PERFIL, ID_PROYECTO, ID_REQUERIMIENTO_ACT))
                .isInstanceOf(PostulacionNoPermitidaException.class)
                .hasMessageContaining("disponibilidad");

        verify(postulacionActividadRepository, never()).saveAndFlush(any());
    }

    @Test
    void actividad_provisional_marcaTentativaEnLaRespuesta() {
        RequerimientoActividad requerimiento = requerimientoActividad(EstadoActividad.PENDIENTE);

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL, ID_USUARIO))
                .thenReturn(Optional.of(perfil(EstadoPerfil.ACTIVO, ID_PROFESION)));
        when(requerimientoActividadRepository.findByIdConActividadYProyecto(ID_REQUERIMIENTO_ACT))
                .thenReturn(Optional.of(requerimiento));
        when(cupoRequerimientoService.calcularActividad(requerimiento))
                .thenReturn(new CupoRequerimiento(1, 0, 1, false));
        when(calendarioService.verificarDisponibilidad(ID_USUARIO, requerimiento.getActividad()))
                .thenReturn(new ResultadoDisponibilidad(true, true, AHORA, AHORA.plusHours(2)));
        when(postulacionActividadRepository.saveAndFlush(any(PostulacionActividad.class)))
                .thenAnswer(invocacion -> {
                    PostulacionActividad p = invocacion.getArgument(0);
                    p.setIdPostulacion(88L);
                    return p;
                });
        when(miembroProyectoRepository.buscarDirectoresDeProyectos(List.of(ID_PROYECTO)))
                .thenReturn(List.of());

        PostulacionResponse respuesta = servicio.darDeAltaPostulacionActividad(
                ID_USUARIO, ID_PERFIL, ID_PROYECTO, ID_REQUERIMIENTO_ACT);

        assertThat(respuesta.tentativa()).isTrue();
    }

    @Test
    void actividad_noPendiente_lanzaExcepcion() {
        RequerimientoActividad requerimiento = requerimientoActividad(EstadoActividad.EN_CURSO);

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL, ID_USUARIO))
                .thenReturn(Optional.of(perfil(EstadoPerfil.ACTIVO, ID_PROFESION)));
        when(requerimientoActividadRepository.findByIdConActividadYProyecto(ID_REQUERIMIENTO_ACT))
                .thenReturn(Optional.of(requerimiento));

        assertThatThrownBy(() -> servicio.darDeAltaPostulacionActividad(
                ID_USUARIO, ID_PERFIL, ID_PROYECTO, ID_REQUERIMIENTO_ACT))
                .isInstanceOf(PostulacionNoPermitidaException.class)
                .hasMessageContaining("pendientes");

        verify(calendarioService, never()).verificarDisponibilidad(any(), any());
    }

    @Test
    void actividad_duplicada_lanzaExcepcion() {
        RequerimientoActividad requerimiento = requerimientoActividad(EstadoActividad.PENDIENTE);

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL, ID_USUARIO))
                .thenReturn(Optional.of(perfil(EstadoPerfil.ACTIVO, ID_PROFESION)));
        when(requerimientoActividadRepository.findByIdConActividadYProyecto(ID_REQUERIMIENTO_ACT))
                .thenReturn(Optional.of(requerimiento));
        when(cupoRequerimientoService.calcularActividad(requerimiento))
                .thenReturn(new CupoRequerimiento(1, 0, 1, false));
        when(postulacionActividadRepository
                .existsByPerfil_IdPerfilAndRequerimiento_IdRequerimiento(
                        ID_PERFIL, ID_REQUERIMIENTO_ACT)).thenReturn(true);

        assertThatThrownBy(() -> servicio.darDeAltaPostulacionActividad(
                ID_USUARIO, ID_PERFIL, ID_PROYECTO, ID_REQUERIMIENTO_ACT))
                .isInstanceOf(PostulacionNoPermitidaException.class)
                .hasMessageContaining("Ya existe una postulación");

        verify(calendarioService, never()).verificarDisponibilidad(any(), any());
    }

    @Test
    void actividad_carreraConOtroAlta_violacionDeUnicidadSeTraduceEnConflicto() {
        RequerimientoActividad requerimiento = requerimientoActividad(EstadoActividad.PENDIENTE);

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL, ID_USUARIO))
                .thenReturn(Optional.of(perfil(EstadoPerfil.ACTIVO, ID_PROFESION)));
        when(requerimientoActividadRepository.findByIdConActividadYProyecto(ID_REQUERIMIENTO_ACT))
                .thenReturn(Optional.of(requerimiento));
        when(cupoRequerimientoService.calcularActividad(requerimiento))
                .thenReturn(new CupoRequerimiento(1, 0, 1, false));
        when(calendarioService.verificarDisponibilidad(ID_USUARIO, requerimiento.getActividad()))
                .thenReturn(new ResultadoDisponibilidad(true, false, AHORA, AHORA.plusHours(2)));
        when(postulacionActividadRepository.saveAndFlush(any(PostulacionActividad.class)))
                .thenThrow(new DataIntegrityViolationException("UQ_perfil_postulacion_act"));

        assertThatThrownBy(() -> servicio.darDeAltaPostulacionActividad(
                ID_USUARIO, ID_PERFIL, ID_PROYECTO, ID_REQUERIMIENTO_ACT))
                .isInstanceOf(PostulacionNoPermitidaException.class)
                .hasMessageContaining("Ya existe una postulación");

        verify(notificacionService, never()).registrarTodas(anyCollection());
    }

    @Test
    void general_proyectoConfirmado_permitePostular() {
        RequerimientoGralProyecto requerimiento = requerimientoGral(
                proyecto(EstadoProyecto.CONFIRMADO, true));

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL, ID_USUARIO))
                .thenReturn(Optional.of(perfil(EstadoPerfil.ACTIVO, ID_PROFESION)));
        when(requerimientoGralProyectoRepository.findByIdConProyecto(ID_REQUERIMIENTO_GRAL))
                .thenReturn(Optional.of(requerimiento));
        when(cupoRequerimientoService.calcularGeneral(requerimiento))
                .thenReturn(new CupoRequerimiento(1, 0, 1, false));
        when(postulacionGralRepository.saveAndFlush(any(PostulacionGral.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(miembroProyectoRepository.buscarDirectoresDeProyectos(List.of(ID_PROYECTO)))
                .thenReturn(List.of());

        PostulacionResponse respuesta = servicio.darDeAltaPostulacionGeneral(
                ID_USUARIO, ID_PERFIL, ID_PROYECTO, ID_REQUERIMIENTO_GRAL);

        assertThat(respuesta.estado()).isEqualTo(EstadoSolicitud.PENDIENTE);
    }

    @Test
    void general_proyectoConfirmadoConRequerimientoSatisfecho_lanzaExcepcion() {
        RequerimientoGralProyecto requerimiento = requerimientoGral(
                proyecto(EstadoProyecto.CONFIRMADO, true));

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL, ID_USUARIO))
                .thenReturn(Optional.of(perfil(EstadoPerfil.ACTIVO, ID_PROFESION)));
        when(requerimientoGralProyectoRepository.findByIdConProyecto(ID_REQUERIMIENTO_GRAL))
                .thenReturn(Optional.of(requerimiento));
        when(cupoRequerimientoService.calcularGeneral(requerimiento))
                .thenReturn(new CupoRequerimiento(1, 1, 0, true));

        assertThatThrownBy(() -> servicio.darDeAltaPostulacionGeneral(
                ID_USUARIO, ID_PERFIL, ID_PROYECTO, ID_REQUERIMIENTO_GRAL))
                .isInstanceOf(PostulacionNoPermitidaException.class)
                .hasMessageContaining("satisfecho");
    }

    @Test
    void actividad_miembroActivoMiembro_permitePostular() {
        RequerimientoActividad requerimiento = requerimientoActividad(EstadoActividad.PENDIENTE);

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL, ID_USUARIO))
                .thenReturn(Optional.of(perfil(EstadoPerfil.ACTIVO, ID_PROFESION)));
        when(requerimientoActividadRepository.findByIdConActividadYProyecto(ID_REQUERIMIENTO_ACT))
                .thenReturn(Optional.of(requerimiento));
        when(miembroProyectoRepository.findMiembroActivoConRol(ID_PROYECTO, ID_PERFIL))
                .thenReturn(Optional.of(miembro("MIEMBRO")));
        when(cupoRequerimientoService.calcularActividad(requerimiento))
                .thenReturn(new CupoRequerimiento(1, 0, 1, false));
        when(calendarioService.verificarDisponibilidad(ID_USUARIO, requerimiento.getActividad()))
                .thenReturn(new ResultadoDisponibilidad(true, false, AHORA, AHORA.plusHours(2)));
        when(postulacionActividadRepository.saveAndFlush(any(PostulacionActividad.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(miembroProyectoRepository.buscarDirectoresDeProyectos(List.of(ID_PROYECTO)))
                .thenReturn(List.of());

        PostulacionResponse respuesta = servicio.darDeAltaPostulacionActividad(
                ID_USUARIO, ID_PERFIL, ID_PROYECTO, ID_REQUERIMIENTO_ACT);

        assertThat(respuesta.estado()).isEqualTo(EstadoSolicitud.PENDIENTE);
    }

    @Test
    void actividad_directorActivo_lanzaExcepcion() {
        RequerimientoActividad requerimiento = requerimientoActividad(EstadoActividad.PENDIENTE);

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL, ID_USUARIO))
                .thenReturn(Optional.of(perfil(EstadoPerfil.ACTIVO, ID_PROFESION)));
        when(requerimientoActividadRepository.findByIdConActividadYProyecto(ID_REQUERIMIENTO_ACT))
                .thenReturn(Optional.of(requerimiento));
        when(miembroProyectoRepository.findMiembroActivoConRol(ID_PROYECTO, ID_PERFIL))
                .thenReturn(Optional.of(miembro("DIRECTOR")));

        assertThatThrownBy(() -> servicio.darDeAltaPostulacionActividad(
                ID_USUARIO, ID_PERFIL, ID_PROYECTO, ID_REQUERIMIENTO_ACT))
                .isInstanceOf(PostulacionNoPermitidaException.class)
                .hasMessageContaining("director");

        verify(calendarioService, never()).verificarDisponibilidad(any(), any());
    }

    // ---------------------------------------------------------------
    // Datos de prueba
    // ---------------------------------------------------------------

    private Perfil perfil(EstadoPerfil estado, Long idProfesion) {
        return Perfil.builder()
                .idPerfil(ID_PERFIL)
                .nombreArtistico("Luna Diseños")
                .estado(estado)
                .usuario(Usuario.builder().idUsuario(ID_USUARIO).build())
                .profesion(Profesion.builder().idProfesion(idProfesion).build())
                .build();
    }

    private Proyecto proyecto(EstadoProyecto estado, boolean aceptaGral) {
        return Proyecto.builder()
                .idProyecto(ID_PROYECTO)
                .nombre("Desfile Primavera")
                .estado(estado)
                .aceptaPostulacionGral(aceptaGral)
                .build();
    }

    private Proyecto proyectoPublicado(boolean aceptaGral) {
        return proyecto(EstadoProyecto.PUBLICADO, aceptaGral);
    }

    private RequerimientoGralProyecto requerimientoGral(Proyecto proyecto) {
        return RequerimientoGralProyecto.builder()
                .idRequerimientoGral(ID_REQUERIMIENTO_GRAL)
                .cantidad(1)
                .proyecto(proyecto)
                .profesion(Profesion.builder().idProfesion(ID_PROFESION).build())
                .build();
    }

    private RequerimientoActividad requerimientoActividad(EstadoActividad estado) {
        Proyecto proyecto = proyectoPublicado(false);
        Planificacion planificacion = Planificacion.builder()
                .idPlanificacion(100L)
                .fechaEntrega(LocalDate.of(2026, 11, 30))
                .proyecto(proyecto)
                .build();
        Actividad actividad = Actividad.builder()
                .idActividad(200L)
                .nombre("Confección de vestuario")
                .duracionMinutos(120)
                .estado(estado)
                .planificacion(planificacion)
                .build();

        return RequerimientoActividad.builder()
                .idRequerimiento(ID_REQUERIMIENTO_ACT)
                .cantidad(1)
                .profesion(Profesion.builder().idProfesion(ID_PROFESION).build())
                .actividad(actividad)
                .build();
    }

    private MiembroProyecto director(Long idPerfil) {
        Usuario usuario = Usuario.builder().idUsuario(idPerfil + 100).build();
        Perfil perfil = Perfil.builder()
                .idPerfil(idPerfil)
                .usuario(usuario)
                .build();
        return MiembroProyecto.builder().perfil(perfil).build();
    }

    private MiembroProyecto miembro(String rolCodigo) {
        return MiembroProyecto.builder()
                .rolProyecto(RolProyecto.builder()
                        .codigo(rolCodigo)
                        .nombre(rolCodigo)
                        .build())
                .build();
    }
}
