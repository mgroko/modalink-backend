package org.mgroko.backend.notificaciones.servicio;

import java.lang.reflect.Method;
import java.util.List;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.perfiles.evento.HabilidadEliminadaEvent;
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
class HabilidadEliminadaNotificacionListenerTest {

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private NotificacionService notificacionService;

    @InjectMocks
    private HabilidadEliminadaNotificacionListener listener;

    @Test
    void sinDestinatarios_noConsultaPerfilesNiRegistraNotificaciones() {
        listener.alEliminarHabilidad(evento(List.of()));

        verifyNoInteractions(perfilRepository, notificacionService);
    }

    @Test
    void conDestinatarios_creaUnaNotificacionPorPerfil() {
        when(perfilRepository.findAllById(any()))
                .thenReturn(List.of(perfil(10L, 1L), perfil(11L, 2L)));
        when(notificacionService.crear(
                any(), any(), anyString(), anyString(), anyString(), any()))
                .thenReturn(Notificacion.builder().build());

        listener.alEliminarHabilidad(evento(List.of(10L, 11L, 10L)));

        verify(notificacionService, times(2)).crear(
                any(),
                any(),
                eq(NotificacionService.TIPO_HABILIDAD_ELIMINADA),
                argThat(mensaje -> mensaje.contains("Retoque y Edición Digital")
                        && mensaje.contains("dada de baja")),
                eq("HABILIDAD"),
                eq(55L));
        verify(notificacionService).registrarTodas(argThat(notificaciones ->
                notificaciones.size() == 2));
    }

    @Test
    void perfilInexistente_noGeneraNotificacion() {
        when(perfilRepository.findAllById(any())).thenReturn(List.of());

        listener.alEliminarHabilidad(evento(List.of(10L)));

        verify(notificacionService, never()).crear(
                any(), any(), anyString(), anyString(), anyString(), any());
        verify(notificacionService, never()).registrarTodas(any());
    }

    @Test
    void falloAlNotificar_noSePropaga() {
        when(perfilRepository.findAllById(any()))
                .thenThrow(new RuntimeException("fallo de notificacion"));

        assertThatCode(() -> listener.alEliminarHabilidad(evento(List.of(10L))))
                .doesNotThrowAnyException();
    }

    @Test
    void listenerSeEjecutaDespuesDelCommitEnUnaTransaccionNueva() throws Exception {
        Method metodo = HabilidadEliminadaNotificacionListener.class
                .getMethod("alEliminarHabilidad", HabilidadEliminadaEvent.class);

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

    private HabilidadEliminadaEvent evento(List<Long> idsPerfiles) {
        return new HabilidadEliminadaEvent(55L, "Retoque y Edición Digital", idsPerfiles);
    }

    private Perfil perfil(Long idPerfil, Long idUsuario) {
        return Perfil.builder()
                .idPerfil(idPerfil)
                .usuario(Usuario.builder().idUsuario(idUsuario).build())
                .build();
    }
}
