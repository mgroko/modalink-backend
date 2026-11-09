package org.mgroko.backend.conexiones.servicio;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.conexiones.exception.SolicitudConexionNoEncontradaException;
import org.mgroko.backend.conexiones.exception.SolicitudConexionNoPendienteException;
import org.mgroko.backend.conexiones.exception.SolicitudConexionNoPermitidaException;
import org.mgroko.backend.modelo.Genero;
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
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mgroko.backend.repositorio.SolicitudConexionRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CancelarSolicitudConexionServiceTest {

    private static final Long ID_USUARIO = 1L;
    private static final Long ID_PERFIL_ACTIVO = 10L;
    private static final Long ID_SOLICITUD = 100L;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private SolicitudConexionRepository solicitudConexionRepository;

    @Mock
    private NotificacionService notificacionService;

    private CancelarSolicitudConexionService service;

    private Usuario usuario;
    private Perfil perfilActivo;
    private Perfil perfilDestinatario;
    private SolicitudConexion solicitud;

    @BeforeEach
    void setUp() {
        service = new CancelarSolicitudConexionService(
                usuarioRepository,
                perfilRepository,
                solicitudConexionRepository,
                notificacionService);

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

        usuario = Usuario.builder()
                .idUsuario(ID_USUARIO)
                .nombre("Ana")
                .apellido("Perez")
                .dni("12345678")
                .fechaNacimiento(LocalDate.now().minusYears(25))
                .correo("ana@example.com")
                .estado(EstadoUsuario.Activo)
                .rolGlobal(rol)
                .genero(genero)
                .build();

        perfilActivo = Perfil.builder()
                .idPerfil(ID_PERFIL_ACTIVO)
                .nombreArtistico("Ana Moda")
                .biografia("Perfil activo")
                .estado(EstadoPerfil.ACTIVO)
                .usuario(usuario)
                .profesion(Profesion.builder().idProfesion(1L).codigo("MODELO").nombre("Modelo").build())
                .build();

        perfilDestinatario = Perfil.builder()
                .idPerfil(20L)
                .nombreArtistico("Bruno Estilo")
                .biografia("Perfil destinatario")
                .estado(EstadoPerfil.ACTIVO)
                .usuario(Usuario.builder().idUsuario(2L).build())
                .profesion(Profesion.builder().idProfesion(2L).codigo("FOTOGRAFO").nombre("Fotógrafo").build())
                .build();

        solicitud = SolicitudConexion.builder()
                .idSolicitudConexion(ID_SOLICITUD)
                .perfilRemitente(perfilActivo)
                .perfilDestinatario(perfilDestinatario)
                .estado(EstadoSolicitud.PENDIENTE)
                .build();

        when(usuarioRepository.findById(ID_USUARIO)).thenReturn(Optional.of(usuario));
        when(perfilRepository.findByIdPerfilAndUsuarioIdUsuario(ID_PERFIL_ACTIVO, ID_USUARIO))
                .thenReturn(Optional.of(perfilActivo));
        when(solicitudConexionRepository.findByIdForUpdate(ID_SOLICITUD))
                .thenReturn(Optional.of(solicitud));
    }

    @Test
    void cancelar_solicitudPropiaPendiente_eliminaSolicitudYNotificacion() {
        service.cancelar(ID_USUARIO, ID_PERFIL_ACTIVO, ID_SOLICITUD);

        verify(notificacionService).eliminarPorEntidad(
                NotificacionService.TIPO_SOLICITUD_CONEXION_RECIBIDA,
                NotificacionService.ENTIDAD_SOLICITUD_CONEXION,
                ID_SOLICITUD);
        verify(solicitudConexionRepository).delete(solicitud);
    }

    @Test
    void cancelar_sinPerfilActivo_lanzaPerfilActivoNoSeleccionado() {
        assertThatThrownBy(() -> service.cancelar(ID_USUARIO, null, ID_SOLICITUD))
                .isInstanceOf(PerfilActivoNoSeleccionadoException.class);

        verify(solicitudConexionRepository, never())
                .delete(org.mockito.ArgumentMatchers.any(SolicitudConexion.class));
    }

    @Test
    void cancelar_solicitudInexistente_lanzaNoEncontrada() {
        when(solicitudConexionRepository.findByIdForUpdate(ID_SOLICITUD))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelar(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_SOLICITUD))
                .isInstanceOf(SolicitudConexionNoEncontradaException.class);
    }

    @Test
    void cancelar_solicitudDeOtroPerfil_lanzaNoPermitida() {
        solicitud.setPerfilRemitente(perfilDestinatario);

        assertThatThrownBy(() -> service.cancelar(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_SOLICITUD))
                .isInstanceOf(SolicitudConexionNoPermitidaException.class);

        verify(solicitudConexionRepository, never())
                .delete(org.mockito.ArgumentMatchers.any(SolicitudConexion.class));
    }

    @Test
    void cancelar_solicitudNoPendiente_lanzaNoPendiente() {
        solicitud.setEstado(EstadoSolicitud.ACEPTADA);

        assertThatThrownBy(() -> service.cancelar(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_SOLICITUD))
                .isInstanceOf(SolicitudConexionNoPendienteException.class);

        verify(notificacionService, never()).eliminarPorEntidad(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
        verify(solicitudConexionRepository, never())
                .delete(org.mockito.ArgumentMatchers.any(SolicitudConexion.class));
    }

    @Test
    void cancelar_perfilActivoEnBaja_lanzaPerfilEnBaja() {
        perfilActivo.setEstado(EstadoPerfil.PendienteBaja);

        assertThatThrownBy(() -> service.cancelar(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_SOLICITUD))
                .isInstanceOf(PerfilEnBajaException.class);
    }
}
