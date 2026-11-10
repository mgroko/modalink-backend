package org.mgroko.backend.mensajeria.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mgroko.backend.mensajeria.dto.ConversacionResponse;
import org.mgroko.backend.mensajeria.dto.EscribiendoEvento;
import org.mgroko.backend.mensajeria.dto.MensajeEvento;
import org.mgroko.backend.mensajeria.dto.MensajeResponse;
import org.mgroko.backend.mensajeria.dto.ResumenConversacionesResponse;
import org.mgroko.backend.mensajeria.exception.ConversacionNoEncontradaException;
import org.mgroko.backend.mensajeria.exception.ConversacionNoPermitidaException;
import org.mgroko.backend.mensajeria.exception.MensajeNoPermitidoException;
import org.mgroko.backend.modelo.Conversacion;
import org.mgroko.backend.modelo.Mensaje;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Profesion;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.perfiles.servicio.PerfilActivoService;
import org.mgroko.backend.repositorio.ConexionRepository;
import org.mgroko.backend.repositorio.ConversacionRepository;
import org.mgroko.backend.repositorio.MensajeRepository;
import org.mgroko.backend.repositorio.NotificacionRepository;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MensajeriaServiceTest {

    private static final Long ID_USUARIO = 1L;
    private static final Long ID_PERFIL_ACTIVO = 10L;
    private static final Long ID_PERFIL_DESTINO = 20L;
    private static final Long ID_CONVERSACION = 30L;

    @Mock
    private PerfilActivoService perfilActivoService;
    @Mock
    private PerfilRepository perfilRepository;
    @Mock
    private ConexionRepository conexionRepository;
    @Mock
    private ConversacionRepository conversacionRepository;
    @Mock
    private MensajeRepository mensajeRepository;
    @Mock
    private NotificacionService notificacionService;
    @Mock
    private NotificacionRepository notificacionRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private MensajeriaService service;
    private Perfil activo;
    private Perfil destino;
    private Conversacion conversacion;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(
                Instant.parse("2026-11-09T15:00:00Z"), ZoneOffset.UTC);
        service = new MensajeriaService(
                perfilActivoService,
                perfilRepository,
                conexionRepository,
                conversacionRepository,
                mensajeRepository,
                notificacionService,
                notificacionRepository,
                eventPublisher,
                reloj);

        activo = perfil(ID_PERFIL_ACTIVO, ID_USUARIO, "Ana Moda");
        destino = perfil(ID_PERFIL_DESTINO, 2L, "Bruno Estilo");
        conversacion = Conversacion.builder()
                .idConversacion(ID_CONVERSACION)
                .perfil1(activo)
                .perfil2(destino)
                .fechaCreacion(LocalDateTime.of(2026, 11, 1, 10, 0))
                .build();

        when(perfilActivoService.resolver(ID_USUARIO, ID_PERFIL_ACTIVO))
                .thenReturn(activo);
        when(perfilRepository.findById(ID_PERFIL_DESTINO))
                .thenReturn(Optional.of(destino));
        when(perfilRepository.getReferenceById(ID_PERFIL_ACTIVO))
                .thenReturn(activo);
        when(perfilRepository.getReferenceById(ID_PERFIL_DESTINO))
                .thenReturn(destino);
        when(conexionRepository.existsActivaEntre(anyLong(), anyLong()))
                .thenReturn(true);
        when(conversacionRepository.findByIdForUpdate(ID_CONVERSACION))
                .thenReturn(Optional.of(conversacion));
        when(conversacionRepository.buscarPorIdConPerfiles(ID_CONVERSACION))
                .thenReturn(Optional.of(conversacion));
        when(mensajeRepository.buscarUltimosPorConversaciones(any()))
                .thenReturn(List.of());
        when(mensajeRepository.buscarIdsConNoLeidos(any(), anyLong()))
                .thenReturn(List.of());
        when(mensajeRepository.buscarUltimoIdDeContraparte(anyLong(), anyLong()))
                .thenReturn(Optional.of(50L));
        when(notificacionRepository
                .existsByUsuarioDestino_IdUsuarioAndTipoAndEntidadTipoAndEntidadIdAndLeidaFalse(
                        anyLong(), anyString(), anyString(), anyLong()))
                .thenReturn(false);
        when(notificacionService.crear(any(), any(), anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(new Notificacion());
        when(notificacionService.registrarTodas(any()))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(mensajeRepository.saveAndFlush(any(Mensaje.class)))
                .thenAnswer(invocacion -> {
                    Mensaje mensaje = invocacion.getArgument(0);
                    mensaje.setIdMensaje(50L);
                    return mensaje;
                });
    }

    @Test
    void crearConversacion_creaParCanonico() {
        when(conversacionRepository.buscarPorPar(ID_PERFIL_ACTIVO, ID_PERFIL_DESTINO))
                .thenReturn(Optional.empty());
        when(conversacionRepository.saveAndFlush(any(Conversacion.class)))
                .thenAnswer(invocacion -> {
                    Conversacion creada = invocacion.getArgument(0);
                    creada.setIdConversacion(ID_CONVERSACION);
                    return creada;
                });

        MensajeriaService.ResultadoConversacion resultado = service.crearConversacion(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_PERFIL_DESTINO);

        assertThat(resultado.creada()).isTrue();
        assertThat(resultado.conversacion().idConversacion()).isEqualTo(ID_CONVERSACION);
        assertThat(resultado.conversacion().perfil().idPerfil()).isEqualTo(ID_PERFIL_DESTINO);
        verify(conexionRepository).existsActivaEntre(ID_PERFIL_ACTIVO, ID_PERFIL_DESTINO);
    }

    @Test
    void crearConversacion_reutilizaExistente() {
        when(conversacionRepository.buscarPorPar(ID_PERFIL_ACTIVO, ID_PERFIL_DESTINO))
                .thenReturn(Optional.of(conversacion));

        MensajeriaService.ResultadoConversacion resultado = service.crearConversacion(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_PERFIL_DESTINO);

        assertThat(resultado.creada()).isFalse();
        verify(conversacionRepository, never()).saveAndFlush(any());
    }

    @Test
    void crearConversacion_sinConexionActiva_lanzaNoPermitida() {
        when(conexionRepository.existsActivaEntre(ID_PERFIL_ACTIVO, ID_PERFIL_DESTINO))
                .thenReturn(false);

        assertThatThrownBy(() -> service.crearConversacion(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_PERFIL_DESTINO))
                .isInstanceOf(ConversacionNoPermitidaException.class);
    }

    @Test
    void enviarMensaje_guardaRecortaContenidoYPublicaEvento() {
        MensajeResponse response = service.enviarMensaje(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_CONVERSACION, "  Hola  ");

        assertThat(response.contenido()).isEqualTo("Hola");
        assertThat(response.esMio()).isTrue();
        assertThat(response.leido()).isFalse();
        verify(notificacionService).registrarTodas(any());
        verify(eventPublisher).publishEvent(any(MensajeEvento.class));
    }

    @Test
    void enviarMensaje_conNotificacionNoLeida_noDuplicaNotificacion() {
        when(notificacionRepository
                .existsByUsuarioDestino_IdUsuarioAndTipoAndEntidadTipoAndEntidadIdAndLeidaFalse(
                        anyLong(), anyString(), anyString(), anyLong()))
                .thenReturn(true);

        service.enviarMensaje(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_CONVERSACION, "Hola");

        verify(notificacionService, never()).crear(any(), any(), anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    void enviarMensaje_conConversacionYaNoLeida_noCreaNotificacion() {
        when(mensajeRepository.existeNoLeidoDeContraparte(
                ID_CONVERSACION, ID_PERFIL_ACTIVO))
                .thenReturn(true);

        service.enviarMensaje(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_CONVERSACION, "Hola");

        verify(notificacionService, never()).crear(
                any(), any(), anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    void enviarMensaje_sinConexionActiva_lanzaNoPermitido() {
        when(conexionRepository.existsActivaEntre(anyLong(), anyLong()))
                .thenReturn(false);

        assertThatThrownBy(() -> service.enviarMensaje(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_CONVERSACION, "Hola"))
                .isInstanceOf(MensajeNoPermitidoException.class);
    }

    @Test
    void marcarLeido_marcaMensajesNotificacionYPublicaEvento() {
        when(mensajeRepository.marcarLeidos(
                eq(ID_CONVERSACION), eq(ID_PERFIL_ACTIVO), any(), any()))
                .thenReturn(1);

        service.marcarLeido(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_CONVERSACION, null);

        verify(mensajeRepository).marcarLeidos(
                eq(ID_CONVERSACION), eq(ID_PERFIL_ACTIVO), eq(null), any());
        verify(notificacionRepository).marcarLeidasDeConversacion(
                eq(ID_USUARIO), eq(ID_CONVERSACION), any());
        verify(eventPublisher).publishEvent(any(org.mgroko.backend.mensajeria.dto.ConversacionLeidaEvento.class));
    }

    @Test
    void listarMensajes_noParticipante_lanzaNoEncontrada() {
        Conversacion ajena = Conversacion.builder()
                .idConversacion(ID_CONVERSACION)
                .perfil1(perfil(99L, 9L, "Otro"))
                .perfil2(perfil(100L, 10L, "Otro Dos"))
                .build();
        when(conversacionRepository.buscarPorIdConPerfiles(ID_CONVERSACION))
                .thenReturn(Optional.of(ajena));

        assertThatThrownBy(() -> service.listarMensajes(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_CONVERSACION, 0, 20))
                .isInstanceOf(ConversacionNoEncontradaException.class);
    }

    @Test
    void resumen_devuelveTotales() {
        when(conversacionRepository.contarDePerfil(ID_PERFIL_ACTIVO)).thenReturn(4L);
        when(mensajeRepository.contarConversacionesNoLeidas(ID_PERFIL_ACTIVO)).thenReturn(2L);

        ResumenConversacionesResponse resumen = service.resumen(
                ID_USUARIO, ID_PERFIL_ACTIVO);

        assertThat(resumen.totalConversaciones()).isEqualTo(4);
        assertThat(resumen.conversacionesNoLeidas()).isEqualTo(2);
    }

    @Test
    void listarConversaciones_marcaNoLeidosYPuedeEnviar() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(conversacionRepository.buscarDePerfil(eq(ID_PERFIL_ACTIVO), any()))
                .thenReturn(new PageImpl<>(List.of(conversacion), pageable, 1));
        when(mensajeRepository.buscarIdsConNoLeidos(any(), eq(ID_PERFIL_ACTIVO)))
                .thenReturn(List.of(ID_CONVERSACION));
        when(conexionRepository.buscarIdsContrapartesActivas(ID_PERFIL_ACTIVO))
                .thenReturn(List.of(ID_PERFIL_DESTINO));

        var pagina = service.listarConversaciones(
                ID_USUARIO, ID_PERFIL_ACTIVO, 0, 20);

        assertThat(pagina.contenido()).hasSize(1);
        ConversacionResponse item = pagina.contenido().get(0);
        assertThat(item.tieneNoLeidos()).isTrue();
        assertThat(item.puedeEnviar()).isTrue();
    }

    @Test
    void obtenerConversacion_devuelveLaContraparte() {
        ConversacionResponse response = service.obtenerConversacion(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_CONVERSACION);

        assertThat(response.idConversacion()).isEqualTo(ID_CONVERSACION);
        assertThat(response.perfil().idPerfil()).isEqualTo(ID_PERFIL_DESTINO);
        assertThat(response.puedeEnviar()).isTrue();
    }

    @Test
    void obtenerConversacion_noParticipante_lanzaNoEncontrada() {
        Conversacion ajena = Conversacion.builder()
                .idConversacion(ID_CONVERSACION)
                .perfil1(perfil(99L, 9L, "Otro"))
                .perfil2(perfil(100L, 10L, "Otro Dos"))
                .build();
        when(conversacionRepository.buscarPorIdConPerfiles(ID_CONVERSACION))
                .thenReturn(Optional.of(ajena));

        assertThatThrownBy(() -> service.obtenerConversacion(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_CONVERSACION))
                .isInstanceOf(ConversacionNoEncontradaException.class);
    }

    @Test
    void notificarEscribiendo_publicaEventoAlDestinatario() {
        service.notificarEscribiendo(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_CONVERSACION, true);

        ArgumentCaptor<EscribiendoEvento> captor =
                ArgumentCaptor.forClass(EscribiendoEvento.class);
        verify(eventPublisher).publishEvent(captor.capture());

        EscribiendoEvento evento = captor.getValue();
        assertThat(evento.idConversacion()).isEqualTo(ID_CONVERSACION);
        assertThat(evento.idPerfilEmisor()).isEqualTo(ID_PERFIL_ACTIVO);
        assertThat(evento.idPerfilDestinatario()).isEqualTo(ID_PERFIL_DESTINO);
        assertThat(evento.escribiendo()).isTrue();
    }

    @Test
    void notificarEscribiendo_sinConexionActiva_lanzaNoPermitido() {
        when(conexionRepository.existsActivaEntre(anyLong(), anyLong()))
                .thenReturn(false);

        assertThatThrownBy(() -> service.notificarEscribiendo(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_CONVERSACION, true))
                .isInstanceOf(MensajeNoPermitidoException.class);
    }

    @Test
    void notificarEscribiendo_noParticipante_lanzaNoEncontrada() {
        Conversacion ajena = Conversacion.builder()
                .idConversacion(ID_CONVERSACION)
                .perfil1(perfil(99L, 9L, "Otro"))
                .perfil2(perfil(100L, 10L, "Otro Dos"))
                .build();
        when(conversacionRepository.buscarPorIdConPerfiles(ID_CONVERSACION))
                .thenReturn(Optional.of(ajena));

        assertThatThrownBy(() -> service.notificarEscribiendo(
                ID_USUARIO, ID_PERFIL_ACTIVO, ID_CONVERSACION, true))
                .isInstanceOf(ConversacionNoEncontradaException.class);
    }

    private Perfil perfil(Long idPerfil, Long idUsuario, String nombreArtistico) {
        Usuario usuario = Usuario.builder()
                .idUsuario(idUsuario)
                .nombre(nombreArtistico)
                .apellido("Prueba")
                .estado(EstadoUsuario.Activo)
                .build();

        return Perfil.builder()
                .idPerfil(idPerfil)
                .nombreArtistico(nombreArtistico)
                .biografia("Perfil")
                .estado(EstadoPerfil.Activo)
                .usuario(usuario)
                .profesion(Profesion.builder().idProfesion(1L).nombre("Modelo").build())
                .build();
    }
}
