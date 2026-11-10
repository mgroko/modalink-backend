package org.mgroko.backend.mensajeria.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.mgroko.backend.mensajeria.dto.ChatGrupalResponse;
import org.mgroko.backend.mensajeria.dto.MensajeGrupalEvento;
import org.mgroko.backend.mensajeria.dto.MensajeGrupalResponse;
import org.mgroko.backend.mensajeria.exception.MensajeInvalidoException;
import org.mgroko.backend.mensajeria.exception.MensajeNoPermitidoException;
import org.mgroko.backend.modelo.MensajeProyecto;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RolProyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.modelo.enums.Privacidad;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.servicio.PerfilActivoService;
import org.mgroko.backend.proyectos.servicio.ProyectoSecurityService;
import org.mgroko.backend.repositorio.MensajeProyectoRepository;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mgroko.backend.repositorio.NotificacionRepository;
import org.mgroko.backend.repositorio.ProyectoRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatGrupalServiceTest {

    private static final Long ID_USUARIO = 1L;
    private static final Long ID_PERFIL_ACTIVO = 10L;
    private static final Long ID_PROYECTO = 50L;
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 9, 15, 0);

    @Mock
    private PerfilActivoService perfilActivoService;
    @Mock
    private ProyectoRepository proyectoRepository;
    @Mock
    private MiembroProyectoRepository miembroProyectoRepository;
    @Mock
    private MensajeProyectoRepository mensajeProyectoRepository;
    @Mock
    private ProyectoSecurityService proyectoSecurityService;
    @Mock
    private NotificacionService notificacionService;
    @Mock
    private NotificacionRepository notificacionRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ChatGrupalService service;
    private Perfil activo;
    private Perfil otro;
    private Perfil tercero;
    private Proyecto proyecto;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(
                Instant.parse("2026-11-09T15:00:00Z"), ZoneOffset.UTC);
        service = new ChatGrupalService(
                perfilActivoService,
                proyectoRepository,
                miembroProyectoRepository,
                mensajeProyectoRepository,
                proyectoSecurityService,
                notificacionService,
                notificacionRepository,
                eventPublisher,
                reloj);

        activo = perfil(ID_PERFIL_ACTIVO, ID_USUARIO, "Ana Moda");
        otro = perfil(20L, 2L, "Bruno Estilo");
        tercero = perfil(30L, 3L, "Carla Diseño");
        proyecto = Proyecto.builder()
                .idProyecto(ID_PROYECTO)
                .nombre("Campaña Primavera")
                .descripcion("Proyecto de prueba")
                .estado(EstadoProyecto.PUBLICADO)
                .privacidad(Privacidad.PUBLICO)
                .build();

        when(perfilActivoService.resolver(ID_USUARIO, ID_PERFIL_ACTIVO))
                .thenReturn(activo);
        when(proyectoRepository.findById(ID_PROYECTO))
                .thenReturn(Optional.of(proyecto));
        when(miembroProyectoRepository.buscarActivosDeProyectoParaChat(ID_PROYECTO))
                .thenReturn(List.of(
                        miembro(activo, "DIRECTOR"),
                        miembro(otro, "MIEMBRO"),
                        miembro(tercero, "MIEMBRO")));
        when(notificacionRepository.buscarPerfilesConNotificacionNoLeida(
                anyString(), eq(ID_PROYECTO)))
                .thenReturn(List.of());
        when(notificacionService.crear(
                any(), any(), anyString(), anyString(), anyString(), anyLong(), anyLong()))
                .thenAnswer(invocacion -> new Notificacion());
        when(notificacionService.registrarTodas(any()))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(mensajeProyectoRepository.saveAndFlush(any(MensajeProyecto.class)))
                .thenAnswer(invocacion -> {
                    MensajeProyecto mensaje = invocacion.getArgument(0);
                    mensaje.setIdMensajeProyecto(99L);
                    return mensaje;
                });
    }

    @Test
    void enviarMensaje_valido_persisteYNotificaAlResto() {
        MensajeGrupalResponse response = service.enviarMensaje(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_PROYECTO, "  Hola equipo  ");

        assertThat(response.idMensajeProyecto()).isEqualTo(99L);
        assertThat(response.idProyecto()).isEqualTo(ID_PROYECTO);
        assertThat(response.contenido()).isEqualTo("Hola equipo");
        assertThat(response.esMio()).isTrue();
        assertThat(response.fechaEnvio()).isEqualTo(AHORA);

        verify(notificacionService, times(2)).crear(
                any(), any(), anyString(), anyString(), anyString(), anyLong(), anyLong());
        verify(notificacionService, never()).crear(
                any(), eq(activo), anyString(), anyString(), anyString(), anyLong(), anyLong());

        ArgumentCaptor<MensajeGrupalEvento> evento =
                ArgumentCaptor.forClass(MensajeGrupalEvento.class);
        verify(eventPublisher).publishEvent(evento.capture());
        assertThat(evento.getValue().idProyecto()).isEqualTo(ID_PROYECTO);
        assertThat(evento.getValue().idPerfilEmisor()).isEqualTo(ID_PERFIL_ACTIVO);
        assertThat(evento.getValue().idPerfilesDestinatarios())
                .containsExactly(ID_PERFIL_ACTIVO, 20L, 30L);
    }

    @Test
    void enviarMensaje_contenidoVacio_lanzaMensajeInvalido() {
        assertThatThrownBy(() -> service.enviarMensaje(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_PROYECTO, "   "))
                .isInstanceOf(MensajeInvalidoException.class)
                .hasMessage("El mensaje no puede estar vacío.");

        verify(mensajeProyectoRepository, never()).saveAndFlush(any());
    }

    @Test
    void enviarMensaje_contenidoDemasiadoLargo_lanzaMensajeInvalido() {
        assertThatThrownBy(() -> service.enviarMensaje(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_PROYECTO, "a".repeat(1001)))
                .isInstanceOf(MensajeInvalidoException.class)
                .hasMessage("El mensaje no puede superar los 1000 caracteres.");

        verify(mensajeProyectoRepository, never()).saveAndFlush(any());
    }

    @Test
    void enviarMensaje_proyectoFinalizado_lanzaMensajeNoPermitido() {
        proyecto.setEstado(EstadoProyecto.FINALIZADO);

        assertThatThrownBy(() -> service.enviarMensaje(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_PROYECTO, "Hola"))
                .isInstanceOfSatisfying(
                        MensajeNoPermitidoException.class,
                        ex -> assertThat(ex.getCodigo())
                                .isEqualTo("CHAT_GRUPAL_SOLO_LECTURA"));

        verify(mensajeProyectoRepository, never()).saveAndFlush(any());
    }

    @Test
    void enviarMensaje_noDuplicaNotificacionSiYaEstaNoLeida() {
        when(notificacionRepository.buscarPerfilesConNotificacionNoLeida(
                anyString(), eq(ID_PROYECTO)))
                .thenReturn(List.of(20L));

        service.enviarMensaje(ID_USUARIO, ID_PERFIL_ACTIVO, ID_PROYECTO, "Hola");

        verify(notificacionService, times(1)).crear(
                any(), any(), anyString(), anyString(), anyString(), anyLong(), anyLong());
    }

    @Test
    void obtenerChat_devuelveParticipantesYPuedeEnviar() {
        ChatGrupalResponse response = service.obtenerChat(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_PROYECTO);

        assertThat(response.idProyecto()).isEqualTo(ID_PROYECTO);
        assertThat(response.nombreProyecto()).isEqualTo("Campaña Primavera");
        assertThat(response.estadoProyecto()).isEqualTo("PUBLICADO");
        assertThat(response.puedeEnviar()).isTrue();
        assertThat(response.participantes()).hasSize(3);
        assertThat(response.participantes().get(0).rol()).isEqualTo("DIRECTOR");
    }

    @Test
    void obtenerChat_proyectoFinalizado_permiteLecturaPeroNoEnvio() {
        proyecto.setEstado(EstadoProyecto.FINALIZADO);

        ChatGrupalResponse response = service.obtenerChat(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_PROYECTO);

        assertThat(response.puedeEnviar()).isFalse();
    }

    @Test
    void listarMensajes_devuelvePaginaYEsMio() {
        MensajeProyecto mensaje = MensajeProyecto.builder()
                .idMensajeProyecto(99L)
                .proyecto(proyecto)
                .perfilEmisor(activo)
                .contenido("Hola")
                .fechaEnvio(AHORA)
                .build();
        Pageable pageable = PageRequest.of(0, 50);
        when(mensajeProyectoRepository.buscarDeProyecto(eq(ID_PROYECTO), any()))
                .thenReturn(new PageImpl<>(List.of(mensaje), pageable, 1));

        var pagina = service.listarMensajes(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_PROYECTO, 0, 50);

        assertThat(pagina.contenido()).hasSize(1);
        assertThat(pagina.contenido().get(0).esMio()).isTrue();
        assertThat(pagina.totalElementos()).isEqualTo(1);
    }

    @Test
    void marcarLeido_marcaNotificacionesDelProyecto() {
        service.marcarLeido(ID_USUARIO, ID_PERFIL_ACTIVO, ID_PROYECTO);

        verify(notificacionRepository).marcarLeidasDeChatGrupal(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_PROYECTO, AHORA);
    }

    private Perfil perfil(Long idPerfil, Long idUsuario, String nombreArtistico) {
        Usuario usuario = Usuario.builder()
                .idUsuario(idUsuario)
                .nombre("Nombre " + idUsuario)
                .apellido("Apellido " + idUsuario)
                .estado(EstadoUsuario.ACTIVO)
                .build();
        Profesion profesion = Profesion.builder()
                .idProfesion(idPerfil)
                .nombre("Profesión " + idPerfil)
                .build();

        return Perfil.builder()
                .idPerfil(idPerfil)
                .nombreArtistico(nombreArtistico)
                .estado(EstadoPerfil.ACTIVO)
                .usuario(usuario)
                .profesion(profesion)
                .build();
    }

    private MiembroProyecto miembro(Perfil perfil, String codigoRol) {
        return MiembroProyecto.builder()
                .proyecto(proyecto)
                .perfil(perfil)
                .rolProyecto(RolProyecto.builder().codigo(codigoRol).build())
                .estadoParticipacion(EstadoParticipacion.ACTIVO)
                .build();
    }
}
