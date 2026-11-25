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
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.perfiles.evento.CaracteristicaTecnicaCreadaEvent;
import org.mgroko.backend.perfiles.evento.CaracteristicaTecnicaModificadaEvent;
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
class CaracteristicaTecnicaNotificacionListenerTest {

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private NotificacionService notificacionService;

    @InjectMocks
    private CaracteristicaTecnicaNotificacionListener listener;

    @Test
    void sinPerfiles_noRegistraNotificaciones() {
        when(perfilRepository.findActivosPorProfesionConUsuario(7L)).thenReturn(List.of());

        listener.alCrearCaracteristica(eventoCreacion());

        verify(notificacionService, never()).registrarTodas(any());
        verify(notificacionService, never()).crearOActualizarNoLeida(
                any(), any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void conPerfiles_creaUnaNotificacionPorPerfilDeLaProfesion() {
        when(perfilRepository.findActivosPorProfesionConUsuario(7L))
                .thenReturn(List.of(perfil(10L, 1L), perfil(11L, 2L)));
        when(notificacionService.crearOActualizarNoLeida(
                any(), any(), anyString(), anyString(), anyString(), any()))
                .thenReturn(Notificacion.builder().build());

        listener.alCrearCaracteristica(eventoCreacion());

        verify(notificacionService, times(2)).crearOActualizarNoLeida(
                any(),
                any(),
                eq(NotificacionService.TIPO_CARACTERISTICA_TECNICA_CREADA),
                argThat(mensaje -> mensaje.contains("Altura")
                        && mensaje.contains("Fotógrafo")),
                eq(NotificacionService.ENTIDAD_CARACTERISTICA_TECNICA),
                eq(55L));
        verify(notificacionService).registrarTodas(argThat(notificaciones ->
                notificaciones.size() == 2));
    }

    @Test
    void perfilesDuplicados_seDeduplicanPorPerfil() {
        Perfil repetido = perfil(10L, 1L);
        when(perfilRepository.findActivosPorProfesionConUsuario(7L))
                .thenReturn(List.of(repetido, repetido));
        when(notificacionService.crearOActualizarNoLeida(
                any(), any(), anyString(), anyString(), anyString(), any()))
                .thenReturn(Notificacion.builder().build());

        listener.alModificarCaracteristica(eventoModificacion());

        verify(notificacionService, times(1)).crearOActualizarNoLeida(
                any(), any(), anyString(), anyString(), anyString(), any());
        verify(notificacionService).registrarTodas(argThat(notificaciones ->
                notificaciones.size() == 1));
    }

    @Test
    void perfilSinUsuario_seOmite() {
        when(perfilRepository.findActivosPorProfesionConUsuario(7L))
                .thenReturn(List.of(Perfil.builder().idPerfil(10L).build()));

        listener.alCrearCaracteristica(eventoCreacion());

        verify(notificacionService, never()).crearOActualizarNoLeida(
                any(), any(), anyString(), anyString(), anyString(), any());
        verify(notificacionService, never()).registrarTodas(any());
    }

    @Test
    void modificacion_usaElTipoYElMensajeDeModificacion() {
        when(perfilRepository.findActivosPorProfesionConUsuario(7L))
                .thenReturn(List.of(perfil(10L, 1L)));
        when(notificacionService.crearOActualizarNoLeida(
                any(), any(), anyString(), anyString(), anyString(), any()))
                .thenReturn(Notificacion.builder().build());

        listener.alModificarCaracteristica(eventoModificacion());

        verify(notificacionService).crearOActualizarNoLeida(
                any(),
                any(),
                eq(NotificacionService.TIPO_CARACTERISTICA_TECNICA_MODIFICADA),
                argThat(mensaje -> mensaje.contains("modificó")
                        && mensaje.contains("Altura")),
                eq(NotificacionService.ENTIDAD_CARACTERISTICA_TECNICA),
                eq(55L));
    }

    @Test
    void falloAlNotificar_noSePropaga() {
        when(perfilRepository.findActivosPorProfesionConUsuario(7L))
                .thenThrow(new RuntimeException("fallo de notificacion"));

        assertThatCode(() -> listener.alCrearCaracteristica(eventoCreacion()))
                .doesNotThrowAnyException();
    }

    @Test
    void listenerSeEjecutaDespuesDelCommitEnUnaTransaccionNueva() throws Exception {
        Method metodo = CaracteristicaTecnicaNotificacionListener.class
                .getMethod("alCrearCaracteristica", CaracteristicaTecnicaCreadaEvent.class);

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

    private CaracteristicaTecnicaCreadaEvent eventoCreacion() {
        return new CaracteristicaTecnicaCreadaEvent(
                55L, "ALTURA", "Altura", 7L, "Fotógrafo");
    }

    private CaracteristicaTecnicaModificadaEvent eventoModificacion() {
        return new CaracteristicaTecnicaModificadaEvent(
                55L, "ALTURA", "Altura", 7L, "Fotógrafo", "DATOS");
    }

    private Perfil perfil(Long idPerfil, Long idUsuario) {
        return Perfil.builder()
                .idPerfil(idPerfil)
                .usuario(Usuario.builder().idUsuario(idUsuario).build())
                .build();
    }
}
