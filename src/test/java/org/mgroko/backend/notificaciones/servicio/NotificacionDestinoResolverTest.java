package org.mgroko.backend.notificaciones.servicio;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Notificacion;
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

        return Notificacion.builder()
                .idNotificacion(idNotificacion)
                .tipo(tipo)
                .entidadTipo(entidadTipo)
                .entidadId(entidadId)
                .build();
    }

    @Test
    void proyectoDirecto_usaEntidadIdSinConsultarPostulaciones() {
        Notificacion notificacion = notificacion(
                1L, NotificacionService.TIPO_PROYECTO_CANCELADO, "PROYECTO", 50L);

        Map<Long, Long> resultado = resolver.resolverProyectos(List.of(notificacion));

        assertThat(resultado).containsExactly(Map.entry(1L, 50L));
        verifyNoInteractions(postulacionGralRepository, postulacionActividadRepository);
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
    void agrupaIdsPorTipoYResuelveCadaNotificacion() {
        when(postulacionGralRepository.findProyectoPorIds(Set.of(77L, 78L)))
                .thenReturn(List.of(
                        new FilaProyecto(77L, 105L),
                        new FilaProyecto(78L, 105L)));
        when(postulacionActividadRepository.findProyectoPorIds(Set.of(88L)))
                .thenReturn(List.of(new FilaProyecto(88L, 106L)));

        List<Notificacion> notificaciones = List.of(
                notificacion(1L, "POSTULACION_RECIBIDA", "POSTULACION_GRAL", 77L),
                notificacion(2L, "POSTULACION_RECIBIDA", "POSTULACION_GRAL", 78L),
                notificacion(3L, "POSTULACION_RECIBIDA", "POSTULACION_ACTIVIDAD", 88L),
                notificacion(4L, "PROYECTO_CANCELADO", "PROYECTO", 50L),
                notificacion(5L, "HITO_CUMPLIDO", "HITO", 99L));

        Map<Long, Long> resultado = resolver.resolverProyectos(notificaciones);

        assertThat(resultado).containsExactlyInAnyOrderEntriesOf(Map.of(
                1L, 105L,
                2L, 105L,
                3L, 106L,
                4L, 50L));
    }

    @Test
    void tipoSinDestino_quedaFueraDelMapa() {
        Notificacion notificacion = notificacion(1L, "HITO_CUMPLIDO", "HITO", 99L);

        Map<Long, Long> resultado = resolver.resolverProyectos(List.of(notificacion));

        assertThat(resultado).isEmpty();
        verifyNoInteractions(postulacionGralRepository, postulacionActividadRepository);
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
        verifyNoInteractions(postulacionGralRepository, postulacionActividadRepository);
    }

    @Test
    void notificacionSinEntidadId_seIgnora() {
        Notificacion notificacion = notificacion(
                1L, "POSTULACION_RECIBIDA", "POSTULACION_GRAL", null);

        assertThat(resolver.resolverProyectos(List.of(notificacion))).isEmpty();
        verifyNoInteractions(postulacionGralRepository, postulacionActividadRepository);
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
