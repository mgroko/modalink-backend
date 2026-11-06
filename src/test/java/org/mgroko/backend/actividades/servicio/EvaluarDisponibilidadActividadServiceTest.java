package org.mgroko.backend.actividades.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.calendario.dto.ResultadoDisponibilidad;
import org.mgroko.backend.calendario.servicio.CalendarioService;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.proyectos.exception.ProyectoNoConfirmableException;
import org.mgroko.backend.proyectos.servicio.ReprogramarProyectoService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EvaluarDisponibilidadActividadServiceTest {

    private static final LocalDateTime INICIO =
            LocalDateTime.of(2026, 11, 5, 12, 0);

    @Mock
    private CalendarioService calendarioService;

    @Mock
    private ReprogramarProyectoService reprogramarProyectoService;

    @InjectMocks
    private EvaluarDisponibilidadActividadService service;

    @Test
    void evaluar_confirmadoPendiente_usaSimulacion() {
        Proyecto proyecto = proyecto(EstadoProyecto.CONFIRMADO);
        Actividad actividad = actividad(EstadoActividad.PENDIENTE);
        ResultadoDisponibilidad esperado = resultado(true);

        when(reprogramarProyectoService.simularAsignacion(
                100L, 55L, List.of(1L)))
                .thenReturn(new ReprogramarProyectoService.SimulacionAsignacion(
                        55L,
                        INICIO,
                        INICIO.plusMinutes(60),
                        esperado,
                        List.of()));

        ResultadoDisponibilidad resultado = service.evaluar(
                proyecto, actividad, List.of(1L));

        assertThat(resultado).isEqualTo(esperado);
        verify(calendarioService, never()).verificarDisponibilidadMiembros(
                any(), any());
    }

    @Test
    void evaluar_publicadoPendiente_usaSimulacion() {
        Proyecto proyecto = proyecto(EstadoProyecto.PUBLICADO);
        Actividad actividad = actividad(EstadoActividad.PENDIENTE);
        ResultadoDisponibilidad esperado = resultado(true);

        when(reprogramarProyectoService.simularAsignacion(
                100L, 55L, List.of(1L)))
                .thenReturn(new ReprogramarProyectoService.SimulacionAsignacion(
                        55L,
                        INICIO,
                        INICIO.plusMinutes(60),
                        esperado,
                        List.of()));

        assertThat(service.evaluar(proyecto, actividad, List.of(1L)))
                .isEqualTo(esperado);
    }

    @Test
    void evaluar_publicadoSinInicio_marcaProvisional() {
        Proyecto proyecto = Proyecto.builder()
                .idProyecto(100L)
                .nombre("Proyecto")
                .estado(EstadoProyecto.PUBLICADO)
                .build();
        Actividad actividad = actividad(EstadoActividad.PENDIENTE);
        ResultadoDisponibilidad esperado = resultado(true);

        when(reprogramarProyectoService.simularAsignacion(
                100L, 55L, List.of(1L)))
                .thenReturn(new ReprogramarProyectoService.SimulacionAsignacion(
                        55L,
                        INICIO,
                        INICIO.plusMinutes(60),
                        esperado,
                        List.of()));

        ResultadoDisponibilidad resultado = service.evaluar(
                proyecto, actividad, List.of(1L));

        assertThat(resultado.provisional()).isTrue();
    }

    @Test
    void evaluar_sinDisponibilidad_devuelveCodigo() {
        Proyecto proyecto = proyecto(EstadoProyecto.CONFIRMADO);
        Actividad actividad = actividad(EstadoActividad.PENDIENTE);

        when(reprogramarProyectoService.simularAsignacion(
                100L, 55L, List.of(1L)))
                .thenThrow(new ProyectoNoConfirmableException(
                        "SIN_DISPONIBILIDAD",
                        "Sin hueco común"));

        ResultadoDisponibilidad resultado = service.evaluar(
                proyecto, actividad, List.of(1L));

        assertThat(resultado.disponible()).isFalse();
        assertThat(resultado.codigoMotivo()).isEqualTo("SIN_DISPONIBILIDAD");
        assertThat(resultado.motivo()).isEqualTo("Sin hueco común");
    }

    @Test
    void evaluar_borrador_usaVerificacionClasica() {
        Proyecto proyecto = proyecto(EstadoProyecto.BORRADOR);
        Actividad actividad = actividad(EstadoActividad.PENDIENTE);
        ResultadoDisponibilidad esperado = resultado(true);

        when(calendarioService.verificarDisponibilidadMiembros(
                List.of(1L), actividad))
                .thenReturn(esperado);

        assertThat(service.evaluar(proyecto, actividad, List.of(1L)))
                .isSameAs(esperado);
        verify(reprogramarProyectoService, never()).simularAsignacion(
                any(), any(), any());
    }

    @Test
    void evaluar_actividadNoPendiente_usaVerificacionClasica() {
        Proyecto proyecto = proyecto(EstadoProyecto.CONFIRMADO);
        Actividad actividad = actividad(EstadoActividad.EN_CURSO);
        ResultadoDisponibilidad esperado = resultado(true);

        when(calendarioService.verificarDisponibilidadMiembros(
                List.of(1L), actividad))
                .thenReturn(esperado);

        assertThat(service.evaluar(proyecto, actividad, List.of(1L)))
                .isSameAs(esperado);
        verify(reprogramarProyectoService, never()).simularAsignacion(
                any(), any(), any());
    }

    @Test
    void evaluar_simulacionSinResultado_usaVerificacionClasica() {
        Proyecto proyecto = proyecto(EstadoProyecto.CONFIRMADO);
        Actividad actividad = actividad(EstadoActividad.PENDIENTE);
        ResultadoDisponibilidad esperado = resultado(true);

        when(reprogramarProyectoService.simularAsignacion(
                100L, 55L, List.of(1L)))
                .thenReturn(new ReprogramarProyectoService.SimulacionAsignacion(
                        55L,
                        null,
                        null,
                        null,
                        List.of()));
        when(calendarioService.verificarDisponibilidadMiembros(
                List.of(1L), actividad))
                .thenReturn(esperado);

        assertThat(service.evaluar(proyecto, actividad, List.of(1L)))
                .isSameAs(esperado);
    }

    private Proyecto proyecto(EstadoProyecto estado) {
        return Proyecto.builder()
                .idProyecto(100L)
                .nombre("Proyecto")
                .estado(estado)
                .fechaInicio(INICIO)
                .build();
    }

    private Actividad actividad(EstadoActividad estado) {
        return Actividad.builder()
                .idActividad(55L)
                .nombre("Actividad")
                .estado(estado)
                .duracionMinutos(60)
                .build();
    }

    private ResultadoDisponibilidad resultado(boolean disponible) {
        return new ResultadoDisponibilidad(
                disponible,
                false,
                INICIO,
                INICIO.plusHours(2));
    }
}
