package org.mgroko.backend.notificaciones.servicio;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.HitoRepository;
import org.mgroko.backend.repositorio.InvitacionActividadRepository;
import org.mgroko.backend.repositorio.InvitacionGralRepository;
import org.mgroko.backend.repositorio.PostulacionActividadRepository;
import org.mgroko.backend.repositorio.PostulacionGralRepository;
import org.mgroko.backend.repositorio.proyeccion.ProyectoDeEntidad;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificacionDestinoResolverTest {

    @Mock
    private PostulacionGralRepository postulacionGralRepository;

    @Mock
    private PostulacionActividadRepository postulacionActividadRepository;

    @Mock
    private ActividadRepository actividadRepository;

    @Mock
    private InvitacionGralRepository invitacionGralRepository;

    @Mock
    private InvitacionActividadRepository invitacionActividadRepository;

    @Mock
    private HitoRepository hitoRepository;

    @InjectMocks
    private NotificacionDestinoResolver resolver;

    private record FilaProyecto(Long entidadId, Long idProyecto) implements ProyectoDeEntidad {

        @Override
        public Long getEntidadId() {
            return entidadId;
        }

        @Override
        public Long getIdProyecto() {
            return idProyecto;
        }
    }

    private Notificacion notificacion(
            Long idNotificacion,
            String tipo,
            String entidadTipo,
            Long entidadId) {

        return notificacion(idNotificacion, tipo, entidadTipo, entidadId, null);
    }

    private Notificacion notificacion(
            Long idNotificacion,
            String tipo,
            String entidadTipo,
            Long entidadId,
            Long idProyecto) {

        return Notificacion.builder()
                .idNotificacion(idNotificacion)
                .tipo(tipo)
                .entidadTipo(entidadTipo)
                .entidadId(entidadId)
                .idProyecto(idProyecto)
                .build();
    }

    @Test
    void proyectoDirecto_usaEntidadIdSinConsultarRepositorios() {
        Notificacion notificacion = notificacion(
                1L, NotificacionService.TIPO_PROYECTO_CANCELADO, "PROYECTO", 50L);

        Map<Long, Long> resultado = resolver.resolverProyectos(List.of(notificacion));

        assertThat(resultado).containsExactly(Map.entry(1L, 50L));
        verifyNoInteractions(
                postulacionGralRepository,
                postulacionActividadRepository,
                actividadRepository,
                invitacionGralRepository,
                invitacionActividadRepository,
                hitoRepository);
    }

    @Test
    void idProyectoPersistido_tienePrioridadSobreLaDerivacion() {
        Notificacion notificacion = notificacion(
                1L, NotificacionService.TIPO_ACTIVIDAD_ELIMINADA, "ACTIVIDAD", 99L, 50L);

        Map<Long, Long> resultado = resolver.resolverProyectos(List.of(notificacion));

        assertThat(resultado).containsExactly(Map.entry(1L, 50L));
        verifyNoInteractions(
                postulacionGralRepository,
                postulacionActividadRepository,
                actividadRepository,
                invitacionGralRepository,
                invitacionActividadRepository,
                hitoRepository);
    }

    @Test
    void postulacionGral_resuelveProyectoEnLote() {
        when(postulacionGralRepository.findProyectoPorIds(Set.of(77L)))
                .thenReturn(List.of(new FilaProyecto(77L, 105L)));

        Notificacion notificacion = notificacion(
                1L, NotificacionService.TIPO_POSTULACION_RECIBIDA,
                NotificacionDestinoResolver.ENTIDAD_POSTULACION_GRAL, 77L);

        Map<Long, Long> resultado = resolver.resolverProyectos(List.of(notificacion));

        assertThat(resultado).containsExactly(Map.entry(1L, 105L));
        verify(postulacionActividadRepository, never()).findProyectoPorIds(Set.of(77L));
    }

    @Test
    void postulacionActividad_resuelveProyectoEnLote() {
        when(postulacionActividadRepository.findProyectoPorIds(Set.of(88L)))
                .thenReturn(List.of(new FilaProyecto(88L, 106L)));

        Notificacion notificacion = notificacion(
                2L, NotificacionService.TIPO_POSTULACION_RECIBIDA,
                NotificacionDestinoResolver.ENTIDAD_POSTULACION_ACTIVIDAD, 88L);

        Map<Long, Long> resultado = resolver.resolverProyectos(List.of(notificacion));

        assertThat(resultado).containsExactly(Map.entry(2L, 106L));
        verifyNoInteractions(postulacionGralRepository);
    }

    @Test
    void actividad_resuelveProyectoEnLote() {
        when(actividadRepository.findProyectoPorIds(Set.of(66L)))
                .thenReturn(List.of(new FilaProyecto(66L, 107L)));

        Notificacion notificacion = notificacion(
                1L, NotificacionService.TIPO_ACTIVIDAD_MODIFICADA,
                NotificacionDestinoResolver.ENTIDAD_ACTIVIDAD, 66L);

        Map<Long, Long> resultado = resolver.resolverProyectos(List.of(notificacion));

        assertThat(resultado).containsExactly(Map.entry(1L, 107L));
        verifyNoInteractions(
                postulacionGralRepository,
                postulacionActividadRepository,
                invitacionGralRepository,
                invitacionActividadRepository,
                hitoRepository);
    }

    @Test
    void actividadInexistente_quedaFueraDelMapaSinLanzarExcepcion() {
        when(actividadRepository.findProyectoPorIds(Set.of(999L)))
                .thenReturn(List.of());

        Notificacion notificacion = notificacion(
                1L, NotificacionService.TIPO_ACTIVIDAD_ELIMINADA,
                NotificacionDestinoResolver.ENTIDAD_ACTIVIDAD, 999L);

        Map<Long, Long> resultado = resolver.resolverProyectos(List.of(notificacion));

        assertThat(resultado).isEmpty();
    }

    @Test
    void actividadEliminadaConProyectoPersistido_noFallaAunqueLaActividadNoExista() {
        Notificacion notificacion = notificacion(
                1L, NotificacionService.TIPO_ACTIVIDAD_ELIMINADA,
                NotificacionDestinoResolver.ENTIDAD_ACTIVIDAD, 999L, 55L);

        Map<Long, Long> resultado = resolver.resolverProyectos(List.of(notificacion));

        assertThat(resultado).containsExactly(Map.entry(1L, 55L));
        verifyNoInteractions(actividadRepository);
    }

    @Test
    void invitacionGral_resuelveProyectoEnLote() {
        when(invitacionGralRepository.findProyectoPorIds(Set.of(300L)))
                .thenReturn(List.of(new FilaProyecto(300L, 108L)));

        Notificacion notificacion = notificacion(
                1L, NotificacionService.TIPO_INVITACION_RECHAZADA,
                NotificacionDestinoResolver.ENTIDAD_INVITACION_GRAL, 300L);

        Map<Long, Long> resultado = resolver.resolverProyectos(List.of(notificacion));

        assertThat(resultado).containsExactly(Map.entry(1L, 108L));
    }

    @Test
    void invitacionActividad_resuelveProyectoEnLote() {
        when(invitacionActividadRepository.findProyectoPorIds(Set.of(301L)))
                .thenReturn(List.of(new FilaProyecto(301L, 109L)));

        Notificacion notificacion = notificacion(
                1L, NotificacionService.TIPO_INVITACION_RECHAZADA,
                NotificacionDestinoResolver.ENTIDAD_INVITACION_ACTIVIDAD, 301L);

        Map<Long, Long> resultado = resolver.resolverProyectos(List.of(notificacion));

        assertThat(resultado).containsExactly(Map.entry(1L, 109L));
    }

    @Test
    void hito_resuelveProyectoEnLote() {
        when(hitoRepository.findProyectoPorIds(Set.of(99L)))
                .thenReturn(List.of(new FilaProyecto(99L, 110L)));

        Notificacion notificacion = notificacion(
                1L, NotificacionService.TIPO_HITO_CUMPLIDO,
                NotificacionDestinoResolver.ENTIDAD_HITO, 99L);

        Map<Long, Long> resultado = resolver.resolverProyectos(List.of(notificacion));

        assertThat(resultado).containsExactly(Map.entry(1L, 110L));
    }

    @Test
    void agrupaIdsPorTipoYResuelveCadaNotificacion() {
        when(postulacionGralRepository.findProyectoPorIds(Set.of(77L, 78L)))
                .thenReturn(List.of(
                        new FilaProyecto(77L, 105L),
                        new FilaProyecto(78L, 105L)));
        when(postulacionActividadRepository.findProyectoPorIds(Set.of(88L)))
                .thenReturn(List.of(new FilaProyecto(88L, 106L)));
        when(actividadRepository.findProyectoPorIds(Set.of(66L)))
                .thenReturn(List.of(new FilaProyecto(66L, 107L)));
        when(invitacionGralRepository.findProyectoPorIds(Set.of(300L)))
                .thenReturn(List.of(new FilaProyecto(300L, 108L)));
        when(invitacionActividadRepository.findProyectoPorIds(Set.of(301L)))
                .thenReturn(List.of(new FilaProyecto(301L, 109L)));
        when(hitoRepository.findProyectoPorIds(Set.of(99L)))
                .thenReturn(List.of(new FilaProyecto(99L, 110L)));

        List<Notificacion> notificaciones = List.of(
                notificacion(1L, "POSTULACION_RECIBIDA", "POSTULACION_GRAL", 77L),
                notificacion(2L, "POSTULACION_RECIBIDA", "POSTULACION_GRAL", 78L),
                notificacion(3L, "POSTULACION_RECIBIDA", "POSTULACION_ACTIVIDAD", 88L),
                notificacion(4L, "PROYECTO_CANCELADO", "PROYECTO", 50L),
                notificacion(5L, "ACTIVIDAD_MODIFICADA", "ACTIVIDAD", 66L),
                notificacion(6L, "INVITACION_RECHAZADA", "INVITACION_GRAL", 300L),
                notificacion(7L, "INVITACION_RECHAZADA", "INVITACION_ACTIVIDAD", 301L),
                notificacion(8L, "HITO_CUMPLIDO", "HITO", 99L),
                notificacion(9L, "ACTIVIDAD_ELIMINADA", "ACTIVIDAD", 999L, 55L),
                notificacion(10L, "DESCONOCIDO", "OTRA_COSA", 111L));

        Map<Long, Long> resultado = resolver.resolverProyectos(notificaciones);

        assertThat(resultado).containsExactlyInAnyOrderEntriesOf(Map.of(
                1L, 105L,
                2L, 105L,
                3L, 106L,
                4L, 50L,
                5L, 107L,
                6L, 108L,
                7L, 109L,
                8L, 110L,
                9L, 55L));
    }

    @Test
    void tipoSinDestino_quedaFueraDelMapa() {
        Notificacion notificacion = notificacion(
                1L, NotificacionService.TIPO_HITO_CUMPLIDO,
                NotificacionDestinoResolver.ENTIDAD_HITO, 99L);

        when(hitoRepository.findProyectoPorIds(Set.of(99L))).thenReturn(List.of());

        Map<Long, Long> resultado = resolver.resolverProyectos(List.of(notificacion));

        assertThat(resultado).isEmpty();
    }

    @Test
    void tipoDesconocido_quedaFueraDelMapa() {
        Notificacion notificacion = notificacion(
                1L, "DESCONOCIDO", "OTRA_COSA", 99L);

        Map<Long, Long> resultado = resolver.resolverProyectos(List.of(notificacion));

        assertThat(resultado).isEmpty();
    }

    @Test
    void postulacionInexistente_quedaFueraDelMapa() {
        when(postulacionGralRepository.findProyectoPorIds(Set.of(77L)))
                .thenReturn(List.of());

        Notificacion notificacion = notificacion(
                1L, "POSTULACION_RECIBIDA", "POSTULACION_GRAL", 77L);

        Map<Long, Long> resultado = resolver.resolverProyectos(List.of(notificacion));

        assertThat(resultado).isEmpty();
    }

    @Test
    void listaVacia_devuelveMapaVacioSinConsultar() {
        assertThat(resolver.resolverProyectos(List.of())).isEmpty();
        assertThat(resolver.resolverProyectos(null)).isEmpty();
        verifyNoInteractions(
                postulacionGralRepository,
                postulacionActividadRepository,
                actividadRepository,
                invitacionGralRepository,
                invitacionActividadRepository,
                hitoRepository);
    }

    @Test
    void notificacionSinEntidadIdNiProyecto_seIgnora() {
        Notificacion notificacion = notificacion(
                1L, "POSTULACION_RECIBIDA", "POSTULACION_GRAL", null);

        assertThat(resolver.resolverProyectos(List.of(notificacion))).isEmpty();
        verifyNoInteractions(
                postulacionGralRepository,
                postulacionActividadRepository,
                actividadRepository,
                invitacionGralRepository,
                invitacionActividadRepository,
                hitoRepository);
    }

    @Test
    void notificacionSinEntidadIdConProyectoPersistido_seIncluye() {
        Notificacion notificacion = notificacion(
                1L, "PROYECTO_CANCELADO", null, null, 55L);

        assertThat(resolver.resolverProyectos(List.of(notificacion)))
                .containsExactly(Map.entry(1L, 55L));
        verifyNoInteractions(
                postulacionGralRepository,
                postulacionActividadRepository,
                actividadRepository,
                invitacionGralRepository,
                invitacionActividadRepository,
                hitoRepository);
    }

    @Test
    void resolverProyecto_individual_devuelveElProyecto() {
        when(postulacionGralRepository.findProyectoPorIds(Set.of(77L)))
                .thenReturn(List.of(new FilaProyecto(77L, 105L)));

        Notificacion notificacion = notificacion(
                1L, "POSTULACION_RECIBIDA", "POSTULACION_GRAL", 77L);

        assertThat(resolver.resolverProyecto(notificacion)).isEqualTo(105L);
    }
}
