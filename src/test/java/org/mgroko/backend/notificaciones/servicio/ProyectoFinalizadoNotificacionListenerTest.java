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
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.proyectos.evento.ProyectoFinalizadoEvent;
import org.mgroko.backend.repositorio.MiembroProyectoRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@ExtendWith(MockitoExtension.class)
class ProyectoFinalizadoNotificacionListenerTest {

    private static final Long ID_PROYECTO = 100L;
    private static final Long ID_PERFIL_ACTOR = 10L;

    @Mock
    private MiembroProyectoRepository miembroProyectoRepository;

    @Mock
    private NotificacionService notificacionService;

    @InjectMocks
    private ProyectoFinalizadoNotificacionListener listener;

    @Test
    void alFinalizarProyecto_notificaAlRestoDeMiembrosActivos() {
        MiembroProyecto actor = miembro(perfil(ID_PERFIL_ACTOR, 1L));
        MiembroProyecto miembro = miembro(perfil(20L, 2L));
        MiembroProyecto otroDirector = miembro(perfil(30L, 3L));

        when(miembroProyectoRepository.buscarMiembrosActivosDeProyecto(ID_PROYECTO))
                .thenReturn(List.of(actor, miembro, otroDirector));
        when(notificacionService.crear(
                any(), any(), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(Notificacion.builder().build());

        listener.alFinalizarProyecto(evento(ID_PERFIL_ACTOR, false));

        ArgumentCaptor<List<Notificacion>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(notificacionService).registrarTodas(captor.capture());
        assertThat(captor.getValue()).hasSize(2);

        verify(notificacionService, times(2)).crear(
                any(),
                any(),
                eq(NotificacionService.TIPO_CALIFICACION_SOLICITADA),
                argThat(mensaje -> mensaje.contains("Desfile")
                        && mensaje.contains("Calificá")),
                eq("PROYECTO"),
                eq(ID_PROYECTO),
                eq(ID_PROYECTO));
        verify(notificacionService, never()).crear(
                any(),
                argThat(perfil -> perfil != null
                        && ID_PERFIL_ACTOR.equals(perfil.getIdPerfil())),
                anyString(),
                anyString(),
                anyString(),
                any(),
                any());
    }

    @Test
    void alFinalizarProyecto_prematura_mencionaFinalizacionAnticipada() {
        when(miembroProyectoRepository.buscarMiembrosActivosDeProyecto(ID_PROYECTO))
                .thenReturn(List.of(miembro(perfil(20L, 2L))));
        when(notificacionService.crear(
                any(), any(), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(Notificacion.builder().build());

        listener.alFinalizarProyecto(evento(ID_PERFIL_ACTOR, true));

        verify(notificacionService).crear(
                any(),
                any(),
                eq(NotificacionService.TIPO_CALIFICACION_SOLICITADA),
                argThat(mensaje -> mensaje.contains("forma anticipada")),
                eq("PROYECTO"),
                eq(ID_PROYECTO),
                eq(ID_PROYECTO));
    }

    @Test
    void alFinalizarProyecto_dedupeMismoPerfil() {
        Perfil perfil = perfil(20L, 2L);
        MiembroProyecto primero = miembro(perfil);
        MiembroProyecto repetido = miembro(perfil);

        when(miembroProyectoRepository.buscarMiembrosActivosDeProyecto(ID_PROYECTO))
                .thenReturn(List.of(primero, repetido));
        when(notificacionService.crear(
                any(), any(), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(Notificacion.builder().build());

        listener.alFinalizarProyecto(evento(ID_PERFIL_ACTOR, false));

        verify(notificacionService, times(1)).crear(
                any(),
                any(),
                eq(NotificacionService.TIPO_CALIFICACION_SOLICITADA),
                anyString(),
                eq("PROYECTO"),
                eq(ID_PROYECTO),
                eq(ID_PROYECTO));
    }

    @Test
    void alFinalizarProyecto_sinDestinatarios_registraListaVacia() {
        when(miembroProyectoRepository.buscarMiembrosActivosDeProyecto(ID_PROYECTO))
                .thenReturn(List.of());

        listener.alFinalizarProyecto(evento(ID_PERFIL_ACTOR, false));

        verify(notificacionService).registrarTodas(argThat(lista -> lista.isEmpty()));
        verify(notificacionService, never()).crear(
                any(), any(), anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void alFinalizarProyecto_perfilSinUsuario_seOmite() {
        MiembroProyecto sinUsuario = MiembroProyecto.builder()
                .perfil(Perfil.builder().idPerfil(20L).build())
                .build();

        when(miembroProyectoRepository.buscarMiembrosActivosDeProyecto(ID_PROYECTO))
                .thenReturn(List.of(sinUsuario));

        listener.alFinalizarProyecto(evento(ID_PERFIL_ACTOR, false));

        verify(notificacionService, never()).crear(
                any(), any(), anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void falloAlNotificar_noSePropaga() {
        when(miembroProyectoRepository.buscarMiembrosActivosDeProyecto(ID_PROYECTO))
                .thenThrow(new RuntimeException("fallo de notificacion"));

        assertThatCode(() -> listener.alFinalizarProyecto(evento(ID_PERFIL_ACTOR, false)))
                .doesNotThrowAnyException();
    }

    @Test
    void listenerSeEjecutaDespuesDelCommitEnUnaTransaccionNueva() throws Exception {
        Method metodo = ProyectoFinalizadoNotificacionListener.class
                .getMethod("alFinalizarProyecto", ProyectoFinalizadoEvent.class);

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

    private ProyectoFinalizadoEvent evento(
            Long idPerfilActor,
            boolean finalizacionPrematura) {
        return new ProyectoFinalizadoEvent(
                ID_PROYECTO,
                "Desfile",
                LocalDateTime.of(2026, 11, 5, 15, 0),
                idPerfilActor,
                finalizacionPrematura);
    }

    private MiembroProyecto miembro(Perfil perfil) {
        return MiembroProyecto.builder()
                .perfil(perfil)
                .build();
    }

    private Perfil perfil(Long idPerfil, Long idUsuario) {
        return Perfil.builder()
                .idPerfil(idPerfil)
                .nombreArtistico("Perfil " + idPerfil)
                .usuario(Usuario.builder().idUsuario(idUsuario).build())
                .build();
    }
}
