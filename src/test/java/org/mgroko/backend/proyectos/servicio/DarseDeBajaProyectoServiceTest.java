package org.mgroko.backend.proyectos.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.proyectos.dto.DarseDeBajaProyectoRequest;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.proyectos.exception.EstadoParticipacionInvalidoException;
import org.mgroko.backend.proyectos.exception.EstadoProyectoNoPermiteBajaVoluntariaException;
import org.mgroko.backend.proyectos.exception.MotivoBajaVoluntariaObligatorioException;
import org.mgroko.backend.proyectos.exception.UltimoDirectorActivoException;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DarseDeBajaProyectoServiceTest {

    private PerfilRepository perfilRepository;
    private PlanificacionRepository planificacionRepository;
    private MiembroProyectoRepository miembroProyectoRepository;
    private NotificacionService notificacionService;
    private final Clock reloj = Clock.fixed(
            Instant.parse("2026-10-31T12:00:00Z"),
            ZoneOffset.UTC);

    private DarseDeBajaProyectoService service;

    private Proyecto proyecto;
    private Perfil perfilActor;
    private Perfil perfilDirector;
    private Perfil perfilOtroDirector;
    private MiembroProyecto miembro;
    private MiembroProyecto director;
    private MiembroProyecto otroDirector;
    private RolProyecto rolDirector;
    private RolProyecto rolMiembro;

    @BeforeEach
    void setUp() {
        perfilRepository = org.mockito.Mockito.mock(PerfilRepository.class);
        planificacionRepository = org.mockito.Mockito.mock(PlanificacionRepository.class);
        miembroProyectoRepository = org.mockito.Mockito.mock(MiembroProyectoRepository.class);
        notificacionService = org.mockito.Mockito.mock(NotificacionService.class);

        service = new DarseDeBajaProyectoService(
                perfilRepository,
                planificacionRepository,
                miembroProyectoRepository,
                notificacionService,
                reloj);

        Usuario usuarioActor = Usuario.builder().idUsuario(1L).build();
        Usuario usuarioDirector = Usuario.builder().idUsuario(2L).build();
        Usuario usuarioOtroDirector = Usuario.builder().idUsuario(3L).build();

        perfilActor = Perfil.builder()
                .idPerfil(10L)
                .nombreArtistico("Luna")
                .estado(EstadoPerfil.Activo)
                .usuario(usuarioActor)
                .build();
        perfilDirector = Perfil.builder()
                .idPerfil(20L)
                .nombreArtistico("Director")
                .estado(EstadoPerfil.Activo)
                .usuario(usuarioDirector)
                .build();
        perfilOtroDirector = Perfil.builder()
                .idPerfil(30L)
                .nombreArtistico("Otro director")
                .estado(EstadoPerfil.Activo)
                .usuario(usuarioOtroDirector)
                .build();

        proyecto = Proyecto.builder()
                .idProyecto(100L)
                .nombre("Desfile 2026")
                .estado(EstadoProyecto.PUBLICADO)
                .build();

        Planificacion planificacion = Planificacion.builder()
                .idPlanificacion(50L)
                .proyecto(proyecto)
                .build();

        rolDirector = RolProyecto.builder()
                .idRolProyecto(1L)
                .codigo("DIRECTOR")
                .nombre("Director de proyecto")
                .build();
        rolMiembro = RolProyecto.builder()
                .idRolProyecto(2L)
                .codigo("MIEMBRO")
                .nombre("Miembro de proyecto")
                .build();

        miembro = miembro(900L, perfilActor, rolMiembro);
        director = miembro(901L, perfilDirector, rolDirector);
        otroDirector = miembro(902L, perfilOtroDirector, rolDirector);

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L))
                .thenReturn(Optional.of(perfilActor));
        when(planificacionRepository.findByProyectoIdProyectoForUpdate(100L))
                .thenReturn(Optional.of(planificacion));
        when(miembroProyectoRepository.findMiembroActivoDeProyectoParaActualizar(100L, 10L))
                .thenReturn(Optional.of(miembro));
        when(miembroProyectoRepository.countOtrosDirectoresActivos(100L, 900L))
                .thenReturn(1L);
        when(miembroProyectoRepository.buscarDirectoresDeProyectos(List.of(100L)))
                .thenReturn(List.of(director));
        when(notificacionService.crear(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Notificacion.builder().build());
    }

    @Test
    void darseDeBaja_guardaEstadoMotivoFechaYNotificaAlDirector() {
        service.darseDeBaja(1L, 10L, 100L,
                new DarseDeBajaProyectoRequest("  Nuevo proyecto  "));

        ArgumentCaptor<MiembroProyecto> captor =
                ArgumentCaptor.forClass(MiembroProyecto.class);
        verify(miembroProyectoRepository).saveAndFlush(captor.capture());

        MiembroProyecto guardado = captor.getValue();
        assertThat(guardado.getEstadoParticipacion())
                .isEqualTo(EstadoParticipacion.BAJA_VOLUNTARIA);
        assertThat(guardado.getMotivoBaja()).isEqualTo("Nuevo proyecto");
        assertThat(guardado.getFechaBaja())
                .isEqualTo(LocalDateTime.of(2026, 10, 31, 12, 0));

        verify(notificacionService).crear(
                eq(perfilDirector.getUsuario()),
                eq(perfilDirector),
                eq(NotificacionService.TIPO_INTEGRANTE_BAJA_VOLUNTARIA),
                contains("Nuevo proyecto"),
                eq("PROYECTO"),
                eq(100L),
                eq(100L));
        verify(notificacionService).registrarTodas(anyList());
    }

    @Test
    void directorConOtroDirector_puedeDarseDeBajaYNoSeAutoNotifica() {
        miembro.setRolProyecto(rolDirector);
        when(miembroProyectoRepository.buscarDirectoresDeProyectos(List.of(100L)))
                .thenReturn(List.of(miembro, otroDirector));

        service.darseDeBaja(1L, 10L, 100L,
                new DarseDeBajaProyectoRequest("Nuevo proyecto"));

        assertThat(miembro.getEstadoParticipacion())
                .isEqualTo(EstadoParticipacion.BAJA_VOLUNTARIA);
        verify(notificacionService).crear(
                eq(perfilOtroDirector.getUsuario()),
                eq(perfilOtroDirector),
                eq(NotificacionService.TIPO_INTEGRANTE_BAJA_VOLUNTARIA),
                any(),
                eq("PROYECTO"),
                eq(100L),
                eq(100L));
        verify(notificacionService, times(1))
                .crear(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void directorUnico_lanzaConflictoYSinGuardar() {
        miembro.setRolProyecto(rolDirector);
        when(miembroProyectoRepository.countOtrosDirectoresActivos(100L, 900L))
                .thenReturn(0L);

        assertThatThrownBy(() -> service.darseDeBaja(
                1L, 10L, 100L, new DarseDeBajaProyectoRequest("Nuevo proyecto")))
                .isInstanceOf(UltimoDirectorActivoException.class);

        verify(miembroProyectoRepository, never()).saveAndFlush(any());
        verify(notificacionService, never()).registrarTodas(anyList());
    }

    @Test
    void proyectoEnEstadoInvalido_lanzaConflicto() {
        proyecto.setEstado(EstadoProyecto.BORRADOR);

        assertThatThrownBy(() -> service.darseDeBaja(
                1L, 10L, 100L, new DarseDeBajaProyectoRequest("Nuevo proyecto")))
                .isInstanceOf(EstadoProyectoNoPermiteBajaVoluntariaException.class);

        verify(miembroProyectoRepository, never()).saveAndFlush(any());
    }

    @Test
    void perfilInactivo_lanzaConflicto() {
        perfilActor.setEstado(EstadoPerfil.Baja);

        assertThatThrownBy(() -> service.darseDeBaja(
                1L, 10L, 100L, new DarseDeBajaProyectoRequest("Nuevo proyecto")))
                .isInstanceOf(PerfilEnBajaException.class);

        verify(miembroProyectoRepository, never()).saveAndFlush(any());
    }

    @Test
    void sinMembresiaActivaConHistorial_lanzaConflicto() {
        when(miembroProyectoRepository.findMiembroActivoDeProyectoParaActualizar(100L, 10L))
                .thenReturn(Optional.empty());
        when(miembroProyectoRepository
                .findFirstByProyectoIdProyectoAndPerfilIdPerfilOrderByFechaAltaDesc(100L, 10L))
                .thenReturn(Optional.of(MiembroProyecto.builder()
                        .idMiembro(900L)
                        .proyecto(proyecto)
                        .perfil(perfilActor)
                        .rolProyecto(rolMiembro)
                        .estadoParticipacion(EstadoParticipacion.BAJA_VOLUNTARIA)
                        .build()));

        assertThatThrownBy(() -> service.darseDeBaja(
                1L, 10L, 100L, new DarseDeBajaProyectoRequest("Nuevo proyecto")))
                .isInstanceOf(EstadoParticipacionInvalidoException.class);

        verify(miembroProyectoRepository, never()).saveAndFlush(any());
    }

    @Test
    void sinMembresia_lanzaAccesoDenegado() {
        when(miembroProyectoRepository.findMiembroActivoDeProyectoParaActualizar(100L, 10L))
                .thenReturn(Optional.empty());
        when(miembroProyectoRepository
                .findFirstByProyectoIdProyectoAndPerfilIdPerfilOrderByFechaAltaDesc(100L, 10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.darseDeBaja(
                1L, 10L, 100L, new DarseDeBajaProyectoRequest("Nuevo proyecto")))
                .isInstanceOf(AccesoDenegadoProyectoException.class);
    }

    @Test
    void motivoVacio_lanzaBadRequest() {
        assertThatThrownBy(() -> service.darseDeBaja(
                1L, 10L, 100L, new DarseDeBajaProyectoRequest("   ")))
                .isInstanceOf(MotivoBajaVoluntariaObligatorioException.class);

        verify(miembroProyectoRepository, never()).saveAndFlush(any());
    }

    @Test
    void motivoLargo_lanzaBadRequest() {
        assertThatThrownBy(() -> service.darseDeBaja(
                1L, 10L, 100L, new DarseDeBajaProyectoRequest("x".repeat(101))))
                .isInstanceOf(MotivoBajaVoluntariaObligatorioException.class);

        verify(miembroProyectoRepository, never()).saveAndFlush(any());
    }

    @Test
    void proyectoSinDirectores_guardaLaBajaSinNotificar() {
        when(miembroProyectoRepository.buscarDirectoresDeProyectos(List.of(100L)))
                .thenReturn(List.of());

        service.darseDeBaja(1L, 10L, 100L,
                new DarseDeBajaProyectoRequest("Nuevo proyecto"));

        verify(miembroProyectoRepository).saveAndFlush(any());
        verify(notificacionService, never()).registrarTodas(anyList());
    }

    private MiembroProyecto miembro(
            Long idMiembro,
            Perfil perfil,
            RolProyecto rol) {
        return MiembroProyecto.builder()
                .idMiembro(idMiembro)
                .proyecto(proyecto)
                .perfil(perfil)
                .rolProyecto(rol)
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .build();
    }
}
