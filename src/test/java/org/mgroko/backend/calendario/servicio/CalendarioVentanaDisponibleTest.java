package org.mgroko.backend.calendario.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.actividades.servicio.CronogramaService;
import org.mgroko.backend.admin.servicio.ConfiguracionSistemaService;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Agenda;
import org.mgroko.backend.modelo.BloqueoAgenda;
import org.mgroko.backend.modelo.JornadaAgenda;
import org.mgroko.backend.modelo.Planificacion;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.modelo.enums.EstadoProyecto;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.AgendaRepository;
import org.mgroko.backend.repositorio.BloqueoAgendaRepository;
import org.mgroko.backend.repositorio.JornadaAgendaRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;

class CalendarioVentanaDisponibleTest {

    private static final Clock RELOJ = Clock.fixed(
            Instant.parse("2026-10-25T12:00:00Z"), ZoneId.of("UTC"));
    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 10, 26, 9, 0);
    private static final LocalDate ENTREGA = LocalDate.of(2026, 11, 30);

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final AgendaRepository agendaRepository = mock(AgendaRepository.class);
    private final JornadaAgendaRepository jornadaAgendaRepository = mock(JornadaAgendaRepository.class);
    private final BloqueoAgendaRepository bloqueoAgendaRepository = mock(BloqueoAgendaRepository.class);
    private final ActividadRepository actividadRepository = mock(ActividadRepository.class);
    private final ConfiguracionSistemaService configuracionSistemaService = mock(ConfiguracionSistemaService.class);
    private final CronogramaService cronogramaService = new CronogramaService(RELOJ);

    private CalendarioService calendarioService;

    @BeforeEach
    void setUp() {
        calendarioService = new CalendarioService(
                usuarioRepository,
                agendaRepository,
                jornadaAgendaRepository,
                bloqueoAgendaRepository,
                actividadRepository,
                configuracionSistemaService,
                cronogramaService);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(Usuario.builder()
                .idUsuario(1L)
                .estado(EstadoUsuario.Activo)
                .build()));
        when(agendaRepository.findByUsuario_IdUsuario(1L)).thenReturn(Optional.of(agenda()));
        when(bloqueoAgendaRepository.findSolapados(eq(10L), any(), any())).thenReturn(List.of());
        when(actividadRepository.findActividadesDeUsuario(eq(1L), anyCollection())).thenReturn(List.of());
    }

    @Test
    void ventanaDisponiblePara_dentroDeJornadaSinBloqueos_devuelveTrue() {
        when(jornadaAgendaRepository.findByAgenda_IdAgendaOrderByDiaSemana(10L))
                .thenReturn(List.of(jornadaCorrida(
                        1, LocalTime.of(9, 0), LocalTime.of(18, 0))));

        boolean disponible = calendarioService.ventanaDisponiblePara(
                List.of(1L), INICIO, INICIO.plusHours(1), null);

        assertThat(disponible).isTrue();
    }

    @Test
    void ventanaDisponiblePara_fueraDeJornada_devuelveFalse() {
        when(jornadaAgendaRepository.findByAgenda_IdAgendaOrderByDiaSemana(10L))
                .thenReturn(List.of(jornadaCorrida(
                        1, LocalTime.of(9, 0), LocalTime.of(18, 0))));

        boolean disponible = calendarioService.ventanaDisponiblePara(
                List.of(1L),
                INICIO.minusHours(1),
                INICIO.minusMinutes(30),
                null);

        assertThat(disponible).isFalse();
    }

    @Test
    void ventanaDisponiblePara_bloqueoManualSolapado_devuelveFalse() {
        when(jornadaAgendaRepository.findByAgenda_IdAgendaOrderByDiaSemana(10L))
                .thenReturn(List.of(jornadaCorrida(
                        1, LocalTime.of(9, 0), LocalTime.of(18, 0))));
        when(bloqueoAgendaRepository.findSolapados(eq(10L), any(), any()))
                .thenReturn(List.of(BloqueoAgenda.builder()
                        .fechaHoraInicio(INICIO.plusMinutes(30))
                        .fechaHoraFin(INICIO.plusHours(2))
                        .motivo("Compromiso")
                        .build()));

        boolean disponible = calendarioService.ventanaDisponiblePara(
                List.of(1L), INICIO, INICIO.plusHours(1), null);

        assertThat(disponible).isFalse();
    }

    @Test
    void ventanaDisponiblePara_otraActividadSolapadaConMargen_devuelveFalse() {
        Planificacion planificacion = planificacion(INICIO);
        Actividad otra = actividad(2L, "Otra", 60, planificacion);
        otra.setFechaHoraInicio(INICIO.plusMinutes(30));

        when(jornadaAgendaRepository.findByAgenda_IdAgendaOrderByDiaSemana(10L))
                .thenReturn(List.of(jornadaCorrida(
                        1, LocalTime.of(9, 0), LocalTime.of(18, 0))));
        when(actividadRepository.findActividadesDeUsuario(eq(1L), anyCollection()))
                .thenReturn(List.of(otra));

        boolean disponible = calendarioService.ventanaDisponiblePara(
                List.of(1L), INICIO, INICIO.plusHours(1), 5L);

        assertThat(disponible).isFalse();
    }

    @Test
    void ventanaDisponiblePara_actividadPropiaNoSeCuentaComoBloqueo() {
        Planificacion planificacion = planificacion(INICIO);
        Actividad propia = actividad(5L, "Propia", 60, planificacion);
        propia.setFechaHoraInicio(INICIO);

        when(jornadaAgendaRepository.findByAgenda_IdAgendaOrderByDiaSemana(10L))
                .thenReturn(List.of(jornadaCorrida(
                        1, LocalTime.of(9, 0), LocalTime.of(18, 0))));
        when(actividadRepository.findActividadesDeUsuario(eq(1L), anyCollection()))
                .thenReturn(List.of(propia));

        boolean disponible = calendarioService.ventanaDisponiblePara(
                List.of(1L), INICIO, INICIO.plusHours(1), 5L);

        assertThat(disponible).isTrue();
    }

    private Agenda agenda() {
        return Agenda.builder()
                .idAgenda(10L)
                .margenActividadMinutos(30)
                .build();
    }

    private Proyecto proyecto(LocalDateTime fechaInicio) {
        return Proyecto.builder()
                .idProyecto(5L)
                .nombre("Proyecto")
                .estado(EstadoProyecto.PUBLICADO)
                .fechaInicio(fechaInicio)
                .build();
    }

    private Planificacion planificacion(LocalDateTime fechaInicio) {
        return Planificacion.builder()
                .idPlanificacion(100L)
                .fechaEntrega(ENTREGA)
                .proyecto(proyecto(fechaInicio))
                .build();
    }

    private Actividad actividad(
            Long id,
            String nombre,
            int duracion,
            Planificacion planificacion) {
        return Actividad.builder()
                .idActividad(id)
                .nombre(nombre)
                .duracionMinutos(duracion)
                .estado(EstadoActividad.PENDIENTE)
                .planificacion(planificacion)
                .build();
    }

    private JornadaAgenda jornadaCorrida(int dia, LocalTime inicio, LocalTime fin) {
        return JornadaAgenda.builder()
                .idJornada((long) dia)
                .agenda(agenda())
                .diaSemana(dia)
                .horaInicioManana(inicio)
                .horaFinTarde(fin)
                .build();
    }
}
