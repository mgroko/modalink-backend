package org.mgroko.backend.proyectos.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.actividades.servicio.CronogramaService;
import org.mgroko.backend.calendario.dto.ResultadoDisponibilidad;
import org.mgroko.backend.calendario.dto.ResultadoPreferencia;
import org.mgroko.backend.calendario.servicio.CalendarioService;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.AsignacionActividad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.OrigenInicio;
import org.mgroko.backend.proyectos.exception.ProyectoNoConfirmableException;
import org.mgroko.backend.repositorio.AsignacionActividadRepository;
import org.mockito.Mockito;

class PlanificadorRecursosServiceTest {

    private static final Clock RELOJ = Clock.fixed(
            Instant.parse("2026-11-01T09:00:00Z"),
            ZoneId.of("UTC"));

    private static final LocalDateTime INICIO =
            LocalDateTime.of(2026, 11, 1, 9, 0);

    private final CalendarioService calendarioService =
            Mockito.mock(CalendarioService.class);
    private final AsignacionActividadRepository asignacionRepository =
            Mockito.mock(AsignacionActividadRepository.class);
    private final CronogramaService cronogramaService =
            new CronogramaService(RELOJ);
    private final PlanificadorRecursosService service =
            new PlanificadorRecursosService(
                    cronogramaService,
                    calendarioService,
                    asignacionRepository);

    @Test
    void planificar_actividadLibre_usaElHuecoDevuelto() {
        Actividad actividad = actividad(1L, "Montaje", 60, EstadoActividad.PENDIENTE);
        Planificacion planificacion = planificacion(LocalDate.of(2026, 11, 10));
        AsignacionActividad asignacion = asignacion(10L, 20L, 30L, actividad);

        when(asignacionRepository.findActivasByActividadIds(anyCollection()))
                .thenReturn(List.of(asignacion));
        LocalDateTime inicioHueco = LocalDateTime.of(2026, 11, 1, 10, 0);
        when(calendarioService.verificarDisponibilidadMiembrosEnRango(
                anyCollection(), eq(actividad), any(), any(), anyMap()))
                .thenReturn(disponible(inicioHueco, 60));

        PlanificadorRecursosService.Resultado resultado =
                service.planificar(planificacion, List.of(actividad), INICIO, false);

        assertThat(resultado.inicios().get(1L)).isEqualTo(inicioHueco);
        assertThat(resultado.calculo().viable()).isTrue();
    }

    @Test
    void planificar_actividadSinAsignacion_lanzaExcepcion() {
        Actividad actividad = actividad(1L, "Montaje", 60, EstadoActividad.PENDIENTE);
        Planificacion planificacion = planificacion(LocalDate.of(2026, 11, 10));

        when(asignacionRepository.findActivasByActividadIds(anyCollection()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.planificar(
                planificacion, List.of(actividad), INICIO, false))
                .isInstanceOf(ProyectoNoConfirmableException.class)
                .extracting("codigo")
                .isEqualTo("ACTIVIDADES_SIN_ASIGNACION");
    }

    @Test
    void planificar_sinDisponibilidad_lanzaExcepcion() {
        Actividad actividad = actividad(1L, "Montaje", 60, EstadoActividad.PENDIENTE);
        Planificacion planificacion = planificacion(LocalDate.of(2026, 11, 10));
        AsignacionActividad asignacion = asignacion(10L, 20L, 30L, actividad);

        when(asignacionRepository.findActivasByActividadIds(anyCollection()))
                .thenReturn(List.of(asignacion));
        when(calendarioService.verificarDisponibilidadMiembrosEnRango(
                anyCollection(), eq(actividad), any(), any(), anyMap()))
                .thenReturn(new ResultadoDisponibilidad(
                        false, false, INICIO, INICIO.plusHours(8),
                        null, null, "SIN_HUECO", List.of()));

        assertThatThrownBy(() -> service.planificar(
                planificacion, List.of(actividad), INICIO, false))
                .isInstanceOf(ProyectoNoConfirmableException.class)
                .extracting("codigo")
                .isEqualTo("SIN_DISPONIBILIDAD");
    }

    @Test
    void planificar_dosFijasSolapadas_lanzaExcepcion() {
        Actividad primera = actividad(1L, "Montaje", 60, EstadoActividad.EN_CURSO);
        primera.setFechaHoraInicio(INICIO);
        Actividad segunda = actividad(2L, "Desmontaje", 60, EstadoActividad.EN_CURSO);
        segunda.setFechaHoraInicio(INICIO.plusMinutes(30));
        Planificacion planificacion = planificacion(LocalDate.of(2026, 11, 10));

        when(asignacionRepository.findActivasByActividadIds(anyCollection()))
                .thenReturn(List.of(
                        asignacion(10L, 20L, 30L, primera),
                        asignacion(11L, 20L, 30L, segunda)));

        assertThatThrownBy(() -> service.planificar(
                planificacion, List.of(primera, segunda), INICIO, true))
                .isInstanceOf(ProyectoNoConfirmableException.class)
                .extracting("codigo")
                .isEqualTo("CRONOGRAMA_NO_VIABLE");
    }

    @Test
    void planificar_entregaAnteriorAlInicio_lanzaExcepcion() {
        Actividad actividad = actividad(1L, "Montaje", 60, EstadoActividad.PENDIENTE);
        Planificacion planificacion = planificacion(LocalDate.of(2026, 10, 30));

        assertThatThrownBy(() -> service.planificar(
                planificacion, List.of(actividad), INICIO, false))
                .isInstanceOf(ProyectoNoConfirmableException.class)
                .extracting("codigo")
                .isEqualTo("FECHA_ENTREGA_INVALIDA");
    }

    @Test
    void planificar_pendienteConfirmada_seTrataComoFija() {
        Actividad fija = actividad(1L, "Fija", 60, EstadoActividad.PENDIENTE);
        fija.setFechaHoraInicio(INICIO.plusHours(2));
        fija.setOrigenInicio(OrigenInicio.CONFIRMADA);
        Planificacion planificacion = planificacion(LocalDate.of(2026, 11, 10));

        when(asignacionRepository.findActivasByActividadIds(anyCollection()))
                .thenReturn(List.of(asignacion(10L, 20L, 30L, fija)));

        PlanificadorRecursosService.Resultado resultado =
                service.planificar(planificacion, List.of(fija), INICIO, true);

        assertThat(resultado.inicios().get(1L)).isEqualTo(INICIO.plusHours(2));
    }

    private ResultadoDisponibilidad disponible(LocalDateTime inicio, int duracion) {
        return new ResultadoDisponibilidad(
                true,
                false,
                INICIO,
                INICIO.plusHours(8),
                inicio,
                inicio.plusMinutes(duracion),
                null,
                List.of(),
                ResultadoPreferencia.SIN_PREFERENCIA,
                null,
                null,
                List.of());
    }

    private Planificacion planificacion(LocalDate entrega) {
        Proyecto proyecto = Proyecto.builder()
                .idProyecto(100L)
                .nombre("Proyecto")
                .fechaInicio(INICIO)
                .estado(EstadoProyecto.PUBLICADO)
                .build();
        return Planificacion.builder()
                .idPlanificacion(200L)
                .fechaEntrega(entrega)
                .proyecto(proyecto)
                .build();
    }

    private Actividad actividad(
            Long id,
            String nombre,
            int duracion,
            EstadoActividad estado) {
        return Actividad.builder()
                .idActividad(id)
                .nombre(nombre)
                .duracionMinutos(duracion)
                .estado(estado)
                .build();
    }

    private AsignacionActividad asignacion(
            Long idAsignacion,
            Long idPerfil,
            Long idUsuario,
            Actividad actividad) {
        Usuario usuario = Usuario.builder()
                .idUsuario(idUsuario)
                .build();
        Perfil perfil = Perfil.builder()
                .idPerfil(idPerfil)
                .usuario(usuario)
                .build();
        MiembroProyecto miembro = MiembroProyecto.builder()
                .idMiembro(idPerfil + 100)
                .perfil(perfil)
                .build();
        return AsignacionActividad.builder()
                .idAsignacionAct(idAsignacion)
                .miembro(miembro)
                .actividad(actividad)
                .build();
    }
}
