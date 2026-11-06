package org.mgroko.backend.notificaciones.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.actividades.evento.ActividadEliminadaEvent;
import org.mgroko.backend.actividades.evento.ActividadEliminadaEvent.InvitacionAfectada;
import org.mgroko.backend.actividades.evento.ActividadEliminadaEvent.PostulacionAfectada;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.repositorio.PerfilRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@ExtendWith(MockitoExtension.class)
class ActividadEliminadaNotificacionListenerTest {

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private NotificacionService notificacionService;

    @InjectMocks
    private ActividadEliminadaNotificacionListener listener;

    @Test
    void sinDestinatarios_noConsultaPerfilesNiRegistraNotificaciones() {
        listener.alEliminarActividad(evento(List.of(), List.of(), List.of()));

        verifyNoInteractions(perfilRepository, notificacionService);
    }

    @Test
    void conAsignadosPostulantesEInvitados_creaNotificacionesConSusTiposYEntidades() {
        Perfil asignado = perfil(10L, 1L);
        Perfil postulante = perfil(11L, 2L);
        Perfil invitado = perfil(12L, 3L);
        when(perfilRepository.findAllById(any()))
                .thenReturn(List.of(asignado, postulante, invitado));
        when(notificacionService.crear(
                any(), any(), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(Notificacion.builder().build());

        listener.alEliminarActividad(evento(
                List.of(10L),
                List.of(new PostulacionAfectada(77L, 11L)),
                List.of(new InvitacionAfectada(88L, 12L))));

        verify(notificacionService).crear(
                any(),
                any(),
                eq(NotificacionService.TIPO_ACTIVIDAD_ELIMINADA),
                argThat(mensaje -> mensaje.contains("Montaje final")
                        && mensaje.contains("Desfile Primavera")
                        && mensaje.contains("Falta de presupuesto")),
                eq("ACTIVIDAD"),
                eq(55L),
                eq(7L));
        verify(notificacionService).crear(
                any(),
                any(),
                eq(NotificacionService.TIPO_POSTULACION_RECHAZADA),
                argThat(mensaje -> mensaje.contains("sin efecto")
                        && mensaje.contains("Falta de presupuesto")),
                eq("POSTULACION_ACTIVIDAD"),
                eq(77L),
                eq(7L));
        verify(notificacionService).crear(
                any(),
                any(),
                eq(NotificacionService.TIPO_INVITACION_RECHAZADA),
                argThat(mensaje -> mensaje.contains("sin efecto")
                        && mensaje.contains("Falta de presupuesto")),
                eq("INVITACION_ACTIVIDAD"),
                eq(88L),
                eq(7L));
        verify(notificacionService).registrarTodas(argThat(notificaciones ->
                notificaciones.size() == 3));
    }

    @Test
    void destinatarioYaNotificadoPorAsignacionYPostulacion_recibeAmbasNotificaciones() {
        Perfil perfil = perfil(10L, 1L);
        when(perfilRepository.findAllById(any())).thenReturn(List.of(perfil));
        when(notificacionService.crear(
                any(), any(), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(Notificacion.builder().build());

        listener.alEliminarActividad(evento(
                List.of(10L),
                List.of(new PostulacionAfectada(77L, 10L)),
                List.of()));

        verify(notificacionService, times(2)).crear(
                any(), any(), anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void perfilInexistente_noGeneraNotificacion() {
        when(perfilRepository.findAllById(any())).thenReturn(List.of());

        listener.alEliminarActividad(evento(
                List.of(10L),
                List.of(new PostulacionAfectada(77L, 11L)),
                List.of(new InvitacionAfectada(88L, 12L))));

        verify(notificacionService, never()).crear(
                any(), any(), anyString(), anyString(), anyString(), any(), any());
        verify(notificacionService, never()).registrarTodas(any());
    }

    @Test
    void falloAlNotificar_noSePropaga() {
        when(perfilRepository.findAllById(any()))
                .thenThrow(new RuntimeException("fallo de notificacion"));

        assertThatCode(() -> listener.alEliminarActividad(evento(
                List.of(10L),
                List.of(),
                List.of())))
                .doesNotThrowAnyException();
    }

    @Test
    void listenerSeEjecutaDespuesDelCommitEnUnaTransaccionNueva() throws Exception {
        Method metodo = ActividadEliminadaNotificacionListener.class
                .getMethod("alEliminarActividad", ActividadEliminadaEvent.class);

        TransactionalEventListener listenerEvento =
                AnnotationUtils.findAnnotation(metodo, TransactionalEventListener.class);
        Transactional transaccion =
                AnnotationUtils.findAnnotation(metodo, Transactional.class);

        assertThat(listenerEvento).isNotNull();
        assertThat(listenerEvento.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(listenerEvento.fallbackExecution()).isTrue();
        assertThat(transaccion).isNotNull();
        assertThat(transaccion.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }

    private ActividadEliminadaEvent evento(
            List<Long> idsPerfilesAsignados,
            List<PostulacionAfectada> postulaciones,
            List<InvitacionAfectada> invitaciones) {
        return new ActividadEliminadaEvent(
                55L,
                7L,
                "Montaje final",
                "Desfile Primavera",
                "Falta de presupuesto",
                idsPerfilesAsignados,
                postulaciones,
                invitaciones);
    }

    private Perfil perfil(Long idPerfil, Long idUsuario) {
        return Perfil.builder()
                .idPerfil(idPerfil)
                .usuario(Usuario.builder().idUsuario(idUsuario).build())
                .build();
    }
}
