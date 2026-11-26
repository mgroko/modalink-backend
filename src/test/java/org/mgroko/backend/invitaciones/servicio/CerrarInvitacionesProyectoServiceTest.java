package org.mgroko.backend.invitaciones.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.InvitacionActividad;
import org.mgroko.backend.modelo.InvitacionGral;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.mgroko.backend.notificaciones.servicio.NotificacionDestinoResolver;
import org.mgroko.backend.notificaciones.servicio.NotificacionService;
import org.mgroko.backend.repositorio.InvitacionActividadRepository;
import org.mgroko.backend.repositorio.InvitacionGralRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CerrarInvitacionesProyectoServiceTest {

    private static final Long ID_PROYECTO = 10L;
    private static final LocalDateTime FECHA =
            LocalDateTime.of(2026, 11, 26, 15, 0);

    @Mock
    private InvitacionGralRepository invitacionGralRepository;

    @Mock
    private InvitacionActividadRepository invitacionActividadRepository;

    @Mock
    private NotificacionService notificacionService;

    @Test
    void cierraGeneralesYActividadesYNotifica() {
        Proyecto proyecto = Proyecto.builder()
                .idProyecto(ID_PROYECTO)
                .nombre("Proyecto finalizado")
                .build();
        Usuario usuario = Usuario.builder().idUsuario(1L).build();
        Perfil destinatario = Perfil.builder()
                .idPerfil(2L)
                .usuario(usuario)
                .build();

        InvitacionGral general = InvitacionGral.builder()
                .idInvitacionGral(100L)
                .proyecto(proyecto)
                .perfilDestinatario(destinatario)
                .estado(EstadoSolicitud.PENDIENTE)
                .build();
        InvitacionActividad actividad = InvitacionActividad.builder()
                .idInvitacion(200L)
                .perfilDestinatario(destinatario)
                .estado(EstadoSolicitud.PENDIENTE)
                .build();

        when(invitacionGralRepository
                .findPendientesDeProyectoConDestinatario(ID_PROYECTO))
                .thenReturn(List.of(general));
        when(invitacionActividadRepository
                .findPendientesDeProyectoConDestinatario(ID_PROYECTO))
                .thenReturn(List.of(actividad));
        when(notificacionService.crear(
                any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Notificacion.builder().build());

        CerrarInvitacionesProyectoService service =
                new CerrarInvitacionesProyectoService(
                        invitacionGralRepository,
                        invitacionActividadRepository,
                        notificacionService);

        var resultado = service.cerrarPendientes(
                proyecto,
                CerrarInvitacionesProyectoService.Motivo.FINALIZACION,
                FECHA);

        assertThat(resultado.generales()).isEqualTo(1);
        assertThat(resultado.actividades()).isEqualTo(1);
        assertThat(resultado.total()).isEqualTo(2);
        assertThat(general.getEstado()).isEqualTo(EstadoSolicitud.RECHAZADA);
        assertThat(general.getFechaRechazo()).isEqualTo(FECHA);
        assertThat(actividad.getEstado()).isEqualTo(EstadoSolicitud.RECHAZADA);
        assertThat(actividad.getFechaRechazo()).isEqualTo(FECHA);

        verify(invitacionGralRepository).saveAll(List.of(general));
        verify(invitacionActividadRepository).saveAll(List.of(actividad));
        verify(notificacionService).eliminarPorEntidad(
                eq(NotificacionService.TIPO_INVITACION_RECIBIDA),
                eq(NotificacionDestinoResolver.ENTIDAD_INVITACION_GRAL),
                eq(100L));
        verify(notificacionService).eliminarPorEntidad(
                eq(NotificacionService.TIPO_INVITACION_RECIBIDA),
                eq(NotificacionDestinoResolver.ENTIDAD_INVITACION_ACTIVIDAD),
                eq(200L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Notificacion>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(notificacionService).registrarTodas(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
    }

    @Test
    void sinInvitacionesPendientesNoNotifica() {
        Proyecto proyecto = Proyecto.builder()
                .idProyecto(ID_PROYECTO)
                .nombre("Proyecto")
                .build();
        when(invitacionGralRepository
                .findPendientesDeProyectoConDestinatario(ID_PROYECTO))
                .thenReturn(List.of());
        when(invitacionActividadRepository
                .findPendientesDeProyectoConDestinatario(ID_PROYECTO))
                .thenReturn(List.of());

        CerrarInvitacionesProyectoService service =
                new CerrarInvitacionesProyectoService(
                        invitacionGralRepository,
                        invitacionActividadRepository,
                        notificacionService);

        var resultado = service.cerrarPendientes(
                proyecto,
                CerrarInvitacionesProyectoService.Motivo.FINALIZACION,
                FECHA);

        assertThat(resultado.total()).isZero();
        verify(notificacionService, org.mockito.Mockito.never())
                .registrarTodas(anyList());
    }
}
