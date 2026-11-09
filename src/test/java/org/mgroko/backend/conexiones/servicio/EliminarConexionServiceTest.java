package org.mgroko.backend.conexiones.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.conexiones.exception.ConexionNoActivaException;
import org.mgroko.backend.conexiones.exception.ConexionNoEncontradaException;
import org.mgroko.backend.modelo.Conexion;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoConexion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.servicio.PerfilActivoService;
import org.mgroko.backend.repositorio.ConexionRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EliminarConexionServiceTest {

    private static final Long ID_USUARIO = 1L;
    private static final Long ID_PERFIL_ACTIVO = 7L;
    private static final Long ID_CONTRAPARTE = 42L;
    private static final Long ID_CONEXION = 55L;

    @Mock
    private PerfilActivoService perfilActivoService;

    @Mock
    private ConexionRepository conexionRepository;

    @Mock
    private NotificacionService notificacionService;

    private EliminarConexionService service;

    private Perfil perfilActivo;
    private Perfil contraparte;
    private Conexion conexion;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(
                Instant.parse("2026-11-09T14:00:00Z"), ZoneOffset.UTC);

        service = new EliminarConexionService(
                perfilActivoService,
                conexionRepository,
                notificacionService,
                reloj);

        perfilActivo = perfil(
                ID_PERFIL_ACTIVO, "Ana Moda", 1L, "Ana", EstadoUsuario.Activo);
        contraparte = perfil(
                ID_CONTRAPARTE, "Bruno Estilo", 2L, "Bruno", EstadoUsuario.Activo);

        conexion = Conexion.builder()
                .idConexion(ID_CONEXION)
                .perfil1(perfilActivo)
                .perfil2(contraparte)
                .estado(EstadoConexion.ACTIVA)
                .fechaInicioConexion(LocalDateTime.of(2026, 11, 1, 10, 0))
                .build();

        when(perfilActivoService.resolver(ID_USUARIO, ID_PERFIL_ACTIVO))
                .thenReturn(perfilActivo);
        when(conexionRepository.findByIdForUpdate(ID_CONEXION))
                .thenReturn(Optional.of(conexion));
        when(conexionRepository.saveAndFlush(any(Conexion.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(notificacionService.crear(any(), any(), any(), any(), any(), any()))
                .thenReturn(new Notificacion());
        when(notificacionService.registrarTodas(any()))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void eliminar_conexionActiva_notificaAAmbasPartesYAplicaBajaLogica() {
        service.eliminar(ID_USUARIO, ID_PERFIL_ACTIVO, ID_CONEXION);

        assertThat(conexion.getEstado()).isEqualTo(EstadoConexion.ELIMINADA);
        assertThat(conexion.getFechaFinConexion())
                .isEqualTo(LocalDateTime.of(2026, 11, 9, 14, 0));
        verify(conexionRepository).saveAndFlush(conexion);
        verify(notificacionService, times(2)).crear(
                any(),
                any(),
                eq(NotificacionService.TIPO_CONEXION_ELIMINADA),
                any(String.class),
                eq(NotificacionService.ENTIDAD_CONEXION),
                eq(ID_CONEXION));
    }

    @Test
    void eliminar_conexionInexistente_lanzaNoEncontrada() {
        when(conexionRepository.findByIdForUpdate(ID_CONEXION))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_CONEXION))
                .isInstanceOf(ConexionNoEncontradaException.class);
    }

    @Test
    void eliminar_conexionAjena_lanzaNoEncontrada() {
        Perfil otro = perfil(99L, "Otro", 3L, "Otro", EstadoUsuario.Activo);
        when(perfilActivoService.resolver(ID_USUARIO, ID_PERFIL_ACTIVO))
                .thenReturn(otro);

        assertThatThrownBy(() -> service.eliminar(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_CONEXION))
                .isInstanceOf(ConexionNoEncontradaException.class);
    }

    @Test
    void eliminar_conexionYaEliminada_lanzaNoActiva() {
        conexion.setEstado(EstadoConexion.ELIMINADA);

        assertThatThrownBy(() -> service.eliminar(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_CONEXION))
                .isInstanceOf(ConexionNoActivaException.class);
    }

    @Test
    void eliminar_conContrapartePendienteBaja_soloNotificaAlActor() {
        contraparte.getUsuario().setEstado(EstadoUsuario.PendienteBaja);

        service.eliminar(ID_USUARIO, ID_PERFIL_ACTIVO, ID_CONEXION);

        verify(notificacionService, times(1)).crear(
                any(),
                any(),
                eq(NotificacionService.TIPO_CONEXION_ELIMINADA),
                any(String.class),
                eq(NotificacionService.ENTIDAD_CONEXION),
                eq(ID_CONEXION));
    }

    @Test
    void eliminar_conContraparteBaja_soloNotificaAlActor() {
        contraparte.getUsuario().setEstado(EstadoUsuario.Baja);

        service.eliminar(ID_USUARIO, ID_PERFIL_ACTIVO, ID_CONEXION);

        verify(notificacionService, times(1)).crear(
                any(),
                any(),
                eq(NotificacionService.TIPO_CONEXION_ELIMINADA),
                any(String.class),
                eq(NotificacionService.ENTIDAD_CONEXION),
                eq(ID_CONEXION));
    }

    @Test
    void eliminar_conContraparteDeshabilitada_soloNotificaAlActor() {
        contraparte.getUsuario().setEstado(EstadoUsuario.Deshabilitado);

        service.eliminar(ID_USUARIO, ID_PERFIL_ACTIVO, ID_CONEXION);

        verify(notificacionService, times(1)).crear(
                any(),
                any(),
                eq(NotificacionService.TIPO_CONEXION_ELIMINADA),
                any(String.class),
                eq(NotificacionService.ENTIDAD_CONEXION),
                eq(ID_CONEXION));
    }

    @Test
    void eliminar_conexionNoActiva_noGuardaNiNotifica() {
        conexion.setEstado(EstadoConexion.ELIMINADA);

        assertThatThrownBy(() -> service.eliminar(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_CONEXION))
                .isInstanceOf(ConexionNoActivaException.class);

        verify(conexionRepository, never()).saveAndFlush(any());
        verify(notificacionService, never()).registrarTodas(any());
    }

    private Perfil perfil(
            Long idPerfil,
            String nombreArtistico,
            Long idUsuario,
            String nombre,
            EstadoUsuario estadoUsuario) {

        Usuario usuario = Usuario.builder()
                .idUsuario(idUsuario)
                .nombre(nombre)
                .apellido("Prueba")
                .estado(estadoUsuario)
                .build();

        return Perfil.builder()
                .idPerfil(idPerfil)
                .nombreArtistico(nombreArtistico)
                .biografia("Perfil de prueba")
                .estado(EstadoPerfil.ACTIVO)
                .usuario(usuario)
                .build();
    }
}
