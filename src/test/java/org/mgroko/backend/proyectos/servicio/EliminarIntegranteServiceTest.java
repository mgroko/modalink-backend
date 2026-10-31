package org.mgroko.backend.proyectos.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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
import org.mgroko.backend.proyectos.dto.EliminarIntegranteRequest;
import org.mgroko.backend.proyectos.exception.AccesoDenegadoProyectoException;
import org.mgroko.backend.proyectos.exception.AutoeliminacionIntegranteException;
import org.mgroko.backend.proyectos.exception.EstadoParticipacionInvalidoException;
import org.mgroko.backend.proyectos.exception.EstadoProyectoNoPermiteEliminarIntegrantesException;
import org.mgroko.backend.proyectos.exception.MiembroProyectoNoEncontradoException;
import org.mgroko.backend.proyectos.exception.MotivoEliminacionIntegranteObligatorioException;
import org.mgroko.backend.proyectos.exception.UltimoDirectorActivoException;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.PlanificacionRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EliminarIntegranteServiceTest {

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private PlanificacionRepository planificacionRepository;

    @Mock
    private MiembroProyectoRepository miembroProyectoRepository;

    @Mock
    private ProyectoSecurityService proyectoSecurityService;

    @Mock
    private NotificacionService notificacionService;

    private final Clock reloj = Clock.fixed(
            Instant.parse("2026-10-31T12:00:00Z"),
            ZoneOffset.UTC);

    private EliminarIntegranteService service;

    private Proyecto proyecto;
    private Perfil perfilActor;
    private Perfil perfilObjetivo;
    private MiembroProyecto miembroObjetivo;
    private RolProyecto rolDirector;
    private RolProyecto rolMiembro;

    @BeforeEach
    void setUp() {
        service = new EliminarIntegranteService(
                perfilRepository,
                planificacionRepository,
                miembroProyectoRepository,
                proyectoSecurityService,
                notificacionService,
                reloj);

        Usuario usuarioActor = Usuario.builder().idUsuario(1L).build();
        Usuario usuarioObjetivo = Usuario.builder().idUsuario(2L).build();

        perfilActor = Perfil.builder()
                .idPerfil(10L)
                .estado(EstadoPerfil.Activo)
                .usuario(usuarioActor)
                .build();
        perfilObjetivo = Perfil.builder()
                .idPerfil(20L)
                .nombreArtistico("Luna")
                .estado(EstadoPerfil.Activo)
                .usuario(usuarioObjetivo)
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

        miembroObjetivo = MiembroProyecto.builder()
                .idMiembro(900L)
                .proyecto(proyecto)
                .perfil(perfilObjetivo)
                .rolProyecto(rolMiembro)
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .build();

        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(10L, 1L))
                .thenReturn(Optional.of(perfilActor));
        when(planificacionRepository.findByProyectoIdProyectoForUpdate(100L))
                .thenReturn(Optional.of(planificacion));
        when(miembroProyectoRepository.findByIdMiembroAndProyectoForUpdate(900L, 100L))
                .thenReturn(Optional.of(miembroObjetivo));
        when(miembroProyectoRepository.countOtrosDirectoresActivos(100L, 900L))
                .thenReturn(1L);
        when(notificacionService.crear(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Notificacion.builder().build());
    }

    @Test
    void eliminar_actualizaMembresiaYNotificaAlEliminado() {
        service.eliminar(1L, 10L, 100L, 900L,
                new EliminarIntegranteRequest("  Falta de compromiso  "));

        ArgumentCaptor<MiembroProyecto> captor = ArgumentCaptor.forClass(MiembroProyecto.class);
        verify(miembroProyectoRepository).saveAndFlush(captor.capture());

        MiembroProyecto guardado = captor.getValue();
        assertThat(guardado.getEstadoParticipacion()).isEqualTo(EstadoParticipacion.ELIMINADO);
        assertThat(guardado.getMotivoBaja()).isEqualTo("Falta de compromiso");
        assertThat(guardado.getFechaBaja()).isEqualTo(LocalDateTime.of(2026, 10, 31, 12, 0));

        verify(notificacionService).crear(
                eq(perfilObjetivo.getUsuario()),
                eq(perfilObjetivo),
                eq(NotificacionService.TIPO_INTEGRANTE_ELIMINADO),
                contains("Falta de compromiso"),
                eq("PROYECTO"),
                eq(100L),
                eq(100L));
        verify(notificacionService).registrarTodas(anyList());
    }

    @Test
    void eliminar_permiteEliminarAOtroDirectorSiQuedaUnoActivo() {
        miembroObjetivo.setRolProyecto(rolDirector);
        when(miembroProyectoRepository.countOtrosDirectoresActivos(100L, 900L))
                .thenReturn(1L);

        service.eliminar(1L, 10L, 100L, 900L,
                new EliminarIntegranteRequest("Renuncia"));

        assertThat(miembroObjetivo.getEstadoParticipacion())
                .isEqualTo(EstadoParticipacion.ELIMINADO);
    }

    @Test
    void eliminar_ultimoDirector_lanzaConflictoYSinGuardar() {
        miembroObjetivo.setRolProyecto(rolDirector);
        when(miembroProyectoRepository.countOtrosDirectoresActivos(100L, 900L))
                .thenReturn(0L);

        assertThatThrownBy(() -> service.eliminar(1L, 10L, 100L, 900L,
                new EliminarIntegranteRequest("Renuncia")))
                .isInstanceOf(UltimoDirectorActivoException.class);

        verify(miembroProyectoRepository, never()).saveAndFlush(any());
    }

    @Test
    void eliminar_autoeliminacion_lanzaConflictoYSinGuardar() {
        perfilObjetivo.setIdPerfil(10L);

        assertThatThrownBy(() -> service.eliminar(1L, 10L, 100L, 900L,
                new EliminarIntegranteRequest("Renuncia")))
                .isInstanceOf(AutoeliminacionIntegranteException.class);

        verify(miembroProyectoRepository, never()).saveAndFlush(any());
    }

    @Test
    void eliminar_motivoVacio_lanzaBadRequest() {
        assertThatThrownBy(() -> service.eliminar(1L, 10L, 100L, 900L,
                new EliminarIntegranteRequest("   ")))
                .isInstanceOf(MotivoEliminacionIntegranteObligatorioException.class);

        verify(miembroProyectoRepository, never()).saveAndFlush(any());
    }

    @Test
    void eliminar_motivoLargo_lanzaBadRequest() {
        String motivo = "x".repeat(101);

        assertThatThrownBy(() -> service.eliminar(1L, 10L, 100L, 900L,
                new EliminarIntegranteRequest(motivo)))
                .isInstanceOf(MotivoEliminacionIntegranteObligatorioException.class);

        verify(miembroProyectoRepository, never()).saveAndFlush(any());
    }

    @Test
    void eliminar_miembroInexistente_lanzaNotFound() {
        when(miembroProyectoRepository.findByIdMiembroAndProyectoForUpdate(900L, 100L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(1L, 10L, 100L, 900L,
                new EliminarIntegranteRequest("Renuncia")))
                .isInstanceOf(MiembroProyectoNoEncontradoException.class);
    }

    @Test
    void eliminar_miembroYaEliminado_lanzaConflicto() {
        miembroObjetivo.setEstadoParticipacion(EstadoParticipacion.ELIMINADO);

        assertThatThrownBy(() -> service.eliminar(1L, 10L, 100L, 900L,
                new EliminarIntegranteRequest("Renuncia")))
                .isInstanceOf(EstadoParticipacionInvalidoException.class);
    }

    @Test
    void eliminar_proyectoEnEstadoInvalido_lanzaConflicto() {
        proyecto.setEstado(EstadoProyecto.BORRADOR);

        assertThatThrownBy(() -> service.eliminar(1L, 10L, 100L, 900L,
                new EliminarIntegranteRequest("Renuncia")))
                .isInstanceOf(EstadoProyectoNoPermiteEliminarIntegrantesException.class);
    }

    @Test
    void eliminar_sinPermiso_lanzaAccesoDenegado() {
        doThrow(new AccesoDenegadoProyectoException("Sin permiso"))
                .when(proyectoSecurityService)
                .validarPermiso(100L, 10L, ProyectoSecurityService.PERMISO_ELIMINAR_INTEGRANTE);

        assertThatThrownBy(() -> service.eliminar(1L, 10L, 100L, 900L,
                new EliminarIntegranteRequest("Renuncia")))
                .isInstanceOf(AccesoDenegadoProyectoException.class);
    }

    @Test
    void eliminar_actorSinPerfilActivo_lanzaConflicto() {
        perfilActor.setEstado(EstadoPerfil.Baja);

        assertThatThrownBy(() -> service.eliminar(1L, 10L, 100L, 900L,
                new EliminarIntegranteRequest("Renuncia")))
                .isInstanceOf(PerfilEnBajaException.class);
    }
}
