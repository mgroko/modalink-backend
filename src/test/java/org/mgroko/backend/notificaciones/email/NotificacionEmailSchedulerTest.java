package org.mgroko.backend.notificaciones.email;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.admin.servicio.ConfiguracionSistemaService;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.NotificacionEntrega;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.notificaciones.servicio.NotificacionDestinoResolver;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificacionEmailSchedulerTest {

    @Mock
    private NotificacionEntregaService entregaService;
    @Mock
    private NotificacionEmailPolicy policy;
    @Mock
    private NotificacionEmailBuilder emailBuilder;
    @Mock
    private NotificacionEmailSender emailSender;
    @Mock
    private TokenAccesoEmailService tokenService;
    @Mock
    private NotificacionDestinoResolver destinoResolver;
    @Mock
    private ConfiguracionSistemaService configuracionSistemaService;

    private NotificacionEmailScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new NotificacionEmailScheduler(
                entregaService,
                policy,
                emailBuilder,
                emailSender,
                tokenService,
                destinoResolver,
                configuracionSistemaService,
                "http://localhost:5173");
    }

    @Test
    void procesaLote_enviaYMarcaEnviado() {
        NotificacionEntrega entrega = entrega();
        when(entregaService.reclamarLote()).thenReturn(List.of(1L));
        when(entregaService.cargar(1L)).thenReturn(Optional.of(entrega));
        when(policy.motivoNoElegible(any(), any())).thenReturn(null);
        when(destinoResolver.resolverProyecto(any())).thenReturn(100L);
        when(tokenService.generarToken(1L, 20L, 30L)).thenReturn("raw");
        when(emailBuilder.construir(any(), eq(100L), eq("raw"), eq("http://localhost:5173"), eq("persona@modalink.com")))
                .thenReturn(new NotificacionEmailBuilder.EmailContent(
                        "persona@modalink.com", "asunto", "<p>", "texto"));

        scheduler.procesarLote();

        verify(emailSender).enviar(any());
        verify(entregaService).marcarEnviado(1L);
    }

    @Test
    void siFallaElEnvio_marcaFallidoConIntentos() {
        NotificacionEntrega entrega = entrega();
        when(entregaService.reclamarLote()).thenReturn(List.of(1L));
        when(entregaService.cargar(1L)).thenReturn(Optional.of(entrega));
        when(policy.motivoNoElegible(any(), any())).thenReturn(null);
        when(destinoResolver.resolverProyecto(any())).thenReturn(100L);
        when(tokenService.generarToken(1L, 20L, 30L)).thenReturn("raw");
        when(emailBuilder.construir(any(), any(), any(), any(), any()))
                .thenReturn(new NotificacionEmailBuilder.EmailContent(
                        "persona@modalink.com", "asunto", "<p>", "texto"));
        doThrow(new RuntimeException("smtp caído")).when(emailSender).enviar(any());

        scheduler.procesarLote();

        verify(entregaService).marcarFallido(eq(1L), eq(1), any(RuntimeException.class));
    }

    @Test
    void siElTipoNoEsElegible_cancelaSinEnviar() {
        NotificacionEntrega entrega = entrega();
        when(entregaService.reclamarLote()).thenReturn(List.of(1L));
        when(entregaService.cargar(1L)).thenReturn(Optional.of(entrega));
        when(policy.motivoNoElegible(any(), any())).thenReturn("TIPO_NO_ELEGIBLE");

        scheduler.procesarLote();

        verify(entregaService).marcarCancelado(1L, "TIPO_NO_ELEGIBLE");
    }

    private NotificacionEntrega entrega() {
        Usuario usuario = Usuario.builder()
                .idUsuario(20L)
                .correo("persona@modalink.com")
                .estado(EstadoUsuario.ACTIVO)
                .build();
        Notificacion notificacion = Notificacion.builder()
                .idNotificacion(10L)
                .tipo(NotificacionService.TIPO_PROYECTO_CONFIRMADO)
                .mensaje("Mensaje")
                .idProyecto(100L)
                .usuarioDestino(usuario)
                .perfilContexto(Perfil.builder().idPerfil(30L).build())
                .build();
        return NotificacionEntrega.builder()
                .idEntrega(1L)
                .intentos(1)
                .notificacion(notificacion)
                .build();
    }
}
