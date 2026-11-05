package org.mgroko.backend.actividades.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.OrigenInicio;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActividadOrigenServiceTest {

    private static final LocalDateTime INICIO =
            LocalDateTime.of(2026, 11, 5, 12, 0);

    @Mock
    private AsignacionActividadRepository asignacionActividadRepository;

    @InjectMocks
    private ActividadOrigenService service;

    @Test
    void calcular_pendienteConInicioYAsignacionActiva_devuelveConfirmada() {
        Actividad actividad = actividad(55L, EstadoProyecto.CONFIRMADO);

        when(asignacionActividadRepository.existsAsignacionActiva(55L))
                .thenReturn(true);

        assertThat(service.calcular(actividad))
                .isEqualTo(OrigenInicio.CONFIRMADA);
    }

    @Test
    void calcular_pendienteSinAsignacionActiva_devuelveProyectada() {
        Actividad actividad = actividad(55L, EstadoProyecto.CONFIRMADO);

        when(asignacionActividadRepository.existsAsignacionActiva(55L))
                .thenReturn(false);

        assertThat(service.calcular(actividad))
                .isEqualTo(OrigenInicio.PROYECTADA);
    }

    @Test
    void calcular_proyectoPublicado_devuelveProyectada() {
        Actividad actividad = actividad(55L, EstadoProyecto.PUBLICADO);

        when(asignacionActividadRepository.existsAsignacionActiva(55L))
                .thenReturn(true);

        assertThat(service.calcular(actividad))
                .isEqualTo(OrigenInicio.PROYECTADA);
    }

    @Test
    void calcular_lista_consultaAsignacionesEnLoteYResuelveCadaFila() {
        Actividad primera = actividad(1L, EstadoProyecto.CONFIRMADO);
        Actividad segunda = actividad(2L, EstadoProyecto.CONFIRMADO);
        primera.setFechaHoraInicio(INICIO);
        segunda.setFechaHoraInicio(INICIO);

        when(asignacionActividadRepository
                .findIdsActividadesConAsignacionesActivas(anyCollection()))
                .thenReturn(Set.of(1L));

        Map<Long, OrigenInicio> origenes = service.calcular(
                List.of(primera, segunda));

        assertThat(origenes.get(1L)).isEqualTo(OrigenInicio.CONFIRMADA);
        assertThat(origenes.get(2L)).isEqualTo(OrigenInicio.PROYECTADA);
        verify(asignacionActividadRepository)
                .findIdsActividadesConAsignacionesActivas(anyCollection());
    }

    private Actividad actividad(Long id, EstadoProyecto estadoProyecto) {
        Proyecto proyecto = Proyecto.builder()
                .idProyecto(100L)
                .estado(estadoProyecto)
                .build();
        Planificacion planificacion = Planificacion.builder()
                .idPlanificacion(50L)
                .proyecto(proyecto)
                .build();
        return Actividad.builder()
                .idActividad(id)
                .nombre("Actividad " + id)
                .estado(EstadoActividad.PENDIENTE)
                .duracionMinutos(60)
                .fechaHoraInicio(INICIO)
                .planificacion(planificacion)
                .build();
    }
}
