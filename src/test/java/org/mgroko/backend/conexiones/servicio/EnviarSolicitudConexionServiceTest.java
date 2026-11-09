package org.mgroko.backend.conexiones.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.auth.exception.UsuarioNoEncontradoException;
import org.mgroko.backend.conexiones.dto.EnviarSolicitudConexionRequest;
import org.mgroko.backend.conexiones.dto.SolicitudConexionResponse;
import org.mgroko.backend.conexiones.exception.ConexionYaExistenteException;
import org.mgroko.backend.conexiones.exception.SolicitudConexionDuplicadaException;
import org.mgroko.backend.conexiones.exception.SolicitudConexionInvalidaException;
import org.mgroko.backend.modelo.Genero;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.RolGlobal;
import org.mgroko.backend.modelo.SolicitudConexion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.exception.PerfilActivoNoSeleccionadoException;
import org.mgroko.backend.perfiles.exception.PerfilEnBajaException;
import org.mgroko.backend.perfiles.exception.PerfilNoEncontradoException;
import org.mgroko.backend.repositorio.ConexionRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.SolicitudConexionRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EnviarSolicitudConexionServiceTest {

    private static final Long ID_USUARIO_A = 1L;
    private static final Long ID_USUARIO_B = 2L;
    private static final Long ID_PERFIL_A = 10L;
    private static final Long ID_PERFIL_B = 20L;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private SolicitudConexionRepository solicitudConexionRepository;

    @Mock
    private ConexionRepository conexionRepository;

    @Mock
    private NotificacionService notificacionService;

    private EnviarSolicitudConexionService service;

    private Usuario usuarioA;
    private Usuario usuarioB;
    private Perfil perfilA;
    private Perfil perfilB;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(
                Instant.parse("2026-11-08T12:00:00Z"), ZoneOffset.UTC);

        var perfilActivoService = new org.mgroko.backend.perfiles.servicio.PerfilActivoService(
                usuarioRepository, perfilRepository);

        service = new EnviarSolicitudConexionService(
                perfilActivoService,
                perfilRepository,
                solicitudConexionRepository,
                conexionRepository,
                notificacionService,
                reloj);

        RolGlobal rol = RolGlobal.builder()
                .idRolGlobal(1L)
                .codigo("USUARIO")
                .nombre("Usuario")
                .build();

        Genero genero = Genero.builder()
                .idGenero(1L)
                .codigo("MUJER")
                .nombre("Mujer")
                .build();

        usuarioA = usuario(ID_USUARIO_A, "Ana", "Perez", rol, genero);
        usuarioB = usuario(ID_USUARIO_B, "Bruno", "Gomez", rol, genero);

        Profesion profesionA = Profesion.builder()
                .idProfesion(1L)
                .codigo("MODELO")
                .nombre("Modelo")
                .build();
        Profesion profesionB = Profesion.builder()
                .idProfesion(2L)
                .codigo("FOTOGRAFO")
                .nombre("Fotógrafo")
                .build();

        perfilA = Perfil.builder()
                .idPerfil(ID_PERFIL_A)
                .nombreArtistico("Ana Moda")
                .biografia("Perfil A")
                .estado(EstadoPerfil.ACTIVO)
                .usuario(usuarioA)
                .profesion(profesionA)
                .build();

        perfilB = Perfil.builder()
                .idPerfil(ID_PERFIL_B)
                .nombreArtistico("Bruno Estilo")
                .biografia("Perfil B")
                .estado(EstadoPerfil.ACTIVO)
                .usuario(usuarioB)
                .profesion(profesionB)
                .build();

        when(usuarioRepository.findById(ID_USUARIO_A)).thenReturn(Optional.of(usuarioA));
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL_A, ID_USUARIO_A))
                .thenReturn(Optional.of(perfilA));
        when(perfilRepository.findById(ID_PERFIL_A)).thenReturn(Optional.of(perfilA));
        when(perfilRepository.findById(ID_PERFIL_B)).thenReturn(Optional.of(perfilB));
        when(conexionRepository.existsActivaEntre(ID_PERFIL_A, ID_PERFIL_B))
                .thenReturn(false);
        when(solicitudConexionRepository.existsPendienteEntre(ID_PERFIL_A, ID_PERFIL_B))
                .thenReturn(false);
        when(solicitudConexionRepository.saveAndFlush(any(SolicitudConexion.class)))
                .thenAnswer(invocacion -> {
                    SolicitudConexion solicitud = invocacion.getArgument(0);
                    solicitud.setIdSolicitudConexion(100L);
                    return solicitud;
                });
        when(notificacionService.crear(any(), any(), any(), any(), any(), any()))
                .thenReturn(new Notificacion());
        when(notificacionService.registrarTodas(any()))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void enviar_creaSolicitudPendienteYNotificacion() {
        SolicitudConexionResponse response = service.enviar(
                ID_USUARIO_A,
                ID_PERFIL_A,
                new EnviarSolicitudConexionRequest(ID_PERFIL_B));

        assertThat(response.idSolicitudConexion()).isEqualTo(100L);
        assertThat(response.estado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(response.remitente().idPerfil()).isEqualTo(ID_PERFIL_A);
        assertThat(response.destinatario().idPerfil()).isEqualTo(ID_PERFIL_B);

        ArgumentCaptor<SolicitudConexion> captor =
                ArgumentCaptor.forClass(SolicitudConexion.class);
        verify(solicitudConexionRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(captor.getValue().getFechaEnvio())
                .isEqualTo(java.time.LocalDateTime.of(2026, 11, 8, 12, 0));

        verify(notificacionService).crear(
                eq(usuarioB),
                eq(perfilB),
                eq(NotificacionService.TIPO_SOLICITUD_CONEXION_RECIBIDA),
                any(String.class),
                eq(NotificacionService.ENTIDAD_SOLICITUD_CONEXION),
                eq(100L));
    }

    @Test
    void enviar_sinPerfilActivo_lanzaPerfilActivoNoSeleccionado() {
        assertThatThrownBy(() -> service.enviar(
                ID_USUARIO_A, null, new EnviarSolicitudConexionRequest(ID_PERFIL_B)))
                .isInstanceOf(PerfilActivoNoSeleccionadoException.class);
    }

    @Test
    void enviar_usuarioNoActivo_lanzaUsuarioNoEncontrado() {
        usuarioA.setEstado(EstadoUsuario.Baja);

        assertThatThrownBy(() -> service.enviar(
                ID_USUARIO_A, ID_PERFIL_A, new EnviarSolicitudConexionRequest(ID_PERFIL_B)))
                .isInstanceOf(UsuarioNoEncontradoException.class);
    }

    @Test
    void enviar_remitenteNoActivo_lanzaPerfilEnBaja() {
        perfilA.setEstado(EstadoPerfil.PendienteBaja);

        assertThatThrownBy(() -> service.enviar(
                ID_USUARIO_A, ID_PERFIL_A, new EnviarSolicitudConexionRequest(ID_PERFIL_B)))
                .isInstanceOf(PerfilEnBajaException.class);
    }

    @Test
    void enviar_destinatarioNoActivo_lanzaPerfilNoEncontrado() {
        perfilB.setEstado(EstadoPerfil.Baja);

        assertThatThrownBy(() -> service.enviar(
                ID_USUARIO_A, ID_PERFIL_A, new EnviarSolicitudConexionRequest(ID_PERFIL_B)))
                .isInstanceOf(PerfilNoEncontradoException.class);
    }

    @Test
    void enviar_mismoPerfil_lanzaSolicitudInvalida() {
        assertThatThrownBy(() -> service.enviar(
                ID_USUARIO_A, ID_PERFIL_A, new EnviarSolicitudConexionRequest(ID_PERFIL_A)))
                .isInstanceOf(SolicitudConexionInvalidaException.class);

        verify(solicitudConexionRepository, never()).saveAndFlush(any());
    }

    @Test
    void enviar_mismoUsuario_lanzaSolicitudInvalida() {
        perfilB.setUsuario(usuarioA);

        assertThatThrownBy(() -> service.enviar(
                ID_USUARIO_A, ID_PERFIL_A, new EnviarSolicitudConexionRequest(ID_PERFIL_B)))
                .isInstanceOf(SolicitudConexionInvalidaException.class);
    }

    @Test
    void enviar_conConexionActiva_lanzaConexionYaExistente() {
        when(conexionRepository.existsActivaEntre(ID_PERFIL_A, ID_PERFIL_B))
                .thenReturn(true);

        assertThatThrownBy(() -> service.enviar(
                ID_USUARIO_A, ID_PERFIL_A, new EnviarSolicitudConexionRequest(ID_PERFIL_B)))
                .isInstanceOf(ConexionYaExistenteException.class);

        verify(solicitudConexionRepository, never()).saveAndFlush(any());
    }

    @Test
    void enviar_conSolicitudPendiente_lanzaDuplicada() {
        when(solicitudConexionRepository.existsPendienteEntre(ID_PERFIL_A, ID_PERFIL_B))
                .thenReturn(true);

        assertThatThrownBy(() -> service.enviar(
                ID_USUARIO_A, ID_PERFIL_A, new EnviarSolicitudConexionRequest(ID_PERFIL_B)))
                .isInstanceOf(SolicitudConexionDuplicadaException.class);
    }

    @Test
    void enviar_duplicadoDetectadoPorIndiceUnico_lanzaDuplicada() {
        when(solicitudConexionRepository.saveAndFlush(any(SolicitudConexion.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "duplicado",
                        new RuntimeException("UQ_solicitud_conexion_pendiente")));

        assertThatThrownBy(() -> service.enviar(
                ID_USUARIO_A, ID_PERFIL_A, new EnviarSolicitudConexionRequest(ID_PERFIL_B)))
                .isInstanceOf(SolicitudConexionDuplicadaException.class);
    }

    @Test
    void enviar_conexionActivaDetectadaPorTrigger_lanzaConexionYaExistente() {
        when(solicitudConexionRepository.saveAndFlush(any(SolicitudConexion.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "conexion activa",
                        new RuntimeException("Ya existe una conexión activa entre los perfiles.")));

        assertThatThrownBy(() -> service.enviar(
                ID_USUARIO_A, ID_PERFIL_A, new EnviarSolicitudConexionRequest(ID_PERFIL_B)))
                .isInstanceOf(ConexionYaExistenteException.class);
    }

    private Usuario usuario(
            Long id,
            String nombre,
            String apellido,
            RolGlobal rol,
            Genero genero) {
        return Usuario.builder()
                .idUsuario(id)
                .nombre(nombre)
                .apellido(apellido)
                .dni("1234567" + id)
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo(nombre.toLowerCase() + "@example.com")
                .estado(EstadoUsuario.Activo)
                .rolGlobal(rol)
                .genero(genero)
                .build();
    }
}
