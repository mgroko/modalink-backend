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
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.actividades.servicio.CronogramaService;
import org.mgroko.backend.admin.servicio.ConfiguracionSistemaService;
import org.mgroko.backend.calendario.dto.ResultadoDisponibilidad;
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

class CalendarioDisponibilidadTest {

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
    void verificarDisponibilidad_huecoExacto_devuelveDisponible() {
        Planificacion planificacion = planificacion(INICIO);
        Actividad actividad = actividad(1L, "A", 120, planificacion);

        when(jornadaAgendaRepository.findByAgenda_IdAgendaOrderByDiaSemana(10L))
                .thenReturn(List.of(jornadaCorrida(1, LocalTime.of(9, 0), LocalTime.of(18, 0))));
        when(actividadRepository.findConPredecesorasByPlanificacion(100L))
                .thenReturn(List.of(actividad));

        ResultadoDisponibilidad resultado = calendarioService.verificarDisponibilidad(1L, actividad);

        assertThat(resultado.disponible()).isTrue();
        assertThat(resultado.provisional()).isFalse();
        assertThat(resultado.inicio()).isEqualTo(INICIO.plusMinutes(30));
        assertThat(resultado.fin()).isEqualTo(INICIO.plusMinutes(150));
    }

    @Test
    void verificarDisponibilidad_bloqueoManualParteLaVentana_devuelveNoDisponible() {
        Planificacion planificacion = planificacion(INICIO);
        planificacion.setFechaEntrega(INICIO.toLocalDate());
        Actividad actividad = actividad(1L, "A", 120, planificacion);

        when(jornadaAgendaRepository.findByAgenda_IdAgendaOrderByDiaSemana(10L))
                .thenReturn(List.of(jornadaCorrida(1, LocalTime.of(9, 0), LocalTime.of(11, 0))));
        when(actividadRepository.findConPredecesorasByPlanificacion(100L))
                .thenReturn(List.of(actividad));
        when(bloqueoAgendaRepository.findSolapados(eq(10L), any(), any()))
                .thenReturn(List.of(BloqueoAgenda.builder()
                        .fechaHoraInicio(INICIO)
                        .fechaHoraFin(INICIO.plusHours(1))
                        .motivo("Compromiso")
                        .build()));

        ResultadoDisponibilidad resultado = calendarioService.verificarDisponibilidad(1L, actividad);

        assertThat(resultado.disponible()).isFalse();
    }

    @Test
    void verificarDisponibilidad_margenDeActividadAsignada_devuelveNoDisponible() {
        Planificacion planificacion = planificacion(INICIO);
        planificacion.setFechaEntrega(INICIO.toLocalDate());
        Actividad actividad = actividad(1L, "A", 120, planificacion);
        Actividad asignada = actividad(2L, "Asignada", 60, planificacion);
        asignada.setFechaHoraInicio(INICIO);

        when(jornadaAgendaRepository.findByAgenda_IdAgendaOrderByDiaSemana(10L))
                .thenReturn(List.of(jornadaCorrida(1, LocalTime.of(9, 0), LocalTime.of(11, 0))));
        when(actividadRepository.findConPredecesorasByPlanificacion(100L))
                .thenReturn(List.of(actividad));
        when(actividadRepository.findActividadesDeUsuario(eq(1L), anyCollection()))
                .thenReturn(List.of(asignada));

        ResultadoDisponibilidad resultado = calendarioService.verificarDisponibilidad(1L, actividad);

        assertThat(resultado.disponible()).isFalse();
    }

    @Test
    void verificarDisponibilidad_conPredecesora_desplazaElRango() {
        Planificacion planificacion = planificacion(INICIO);
        Actividad predecesora = actividad(1L, "A", 60, planificacion);
        Actividad objetivo = actividad(2L, "B", 120, planificacion);
        objetivo.setPredecesoras(Set.of(predecesora));

        when(jornadaAgendaRepository.findByAgenda_IdAgendaOrderByDiaSemana(10L))
                .thenReturn(List.of(jornadaCorrida(1, LocalTime.of(9, 0), LocalTime.of(18, 0))));
        when(actividadRepository.findConPredecesorasByPlanificacion(100L))
                .thenReturn(List.of(predecesora, objetivo));

        ResultadoDisponibilidad resultado = calendarioService.verificarDisponibilidad(1L, objetivo);

        assertThat(resultado.disponible()).isTrue();
        assertThat(resultado.inicio()).isEqualTo(INICIO.plusMinutes(90));
        assertThat(resultado.fin()).isEqualTo(INICIO.plusMinutes(210));
    }

    @Test
    void verificarDisponibilidad_jornadaPartida_usaElBloqueDeLaTarde() {
        Planificacion planificacion = planificacion(INICIO);
        Actividad predecesora = actividad(1L, "A", 300, planificacion);
        Actividad objetivo = actividad(2L, "B", 120, planificacion);
        objetivo.setPredecesoras(Set.of(predecesora));

        when(jornadaAgendaRepository.findByAgenda_IdAgendaOrderByDiaSemana(10L))
                .thenReturn(List.of(jornadaPartida(
                        1,
                        LocalTime.of(9, 0), LocalTime.of(12, 0),
                        LocalTime.of(14, 0), LocalTime.of(18, 0))));
        when(actividadRepository.findConPredecesorasByPlanificacion(100L))
                .thenReturn(List.of(predecesora, objetivo));

        ResultadoDisponibilidad resultado = calendarioService.verificarDisponibilidad(1L, objetivo);

        assertThat(resultado.inicio()).isEqualTo(INICIO.plusMinutes(330));
        assertThat(resultado.disponible()).isTrue();
    }

    @Test
    void verificarDisponibilidad_sinFechaInicio_marcaProvisionalYAbreElRangoHastaLaEntrega() {
        Planificacion planificacion = planificacion(null);
        Actividad actividad = actividad(1L, "A", 120, planificacion);

        when(jornadaAgendaRepository.findByAgenda_IdAgendaOrderByDiaSemana(10L))
                .thenReturn(List.of(jornadaCorrida(7, LocalTime.of(0, 0), LocalTime.of(18, 0))));
        when(actividadRepository.findConPredecesorasByPlanificacion(100L))
                .thenReturn(List.of(actividad));

        ResultadoDisponibilidad resultado = calendarioService.verificarDisponibilidad(1L, actividad);

        assertThat(resultado.provisional()).isTrue();
        assertThat(resultado.inicio()).isEqualTo(LocalDateTime.of(2026, 10, 25, 12, 30));
        assertThat(resultado.finEvaluado()).isEqualTo(ENTREGA.atTime(23, 59));
        assertThat(resultado.finSeleccionado()).isEqualTo(LocalDateTime.of(2026, 10, 25, 14, 30));
        assertThat(resultado.disponible()).isTrue();
    }

    @Test
    void verificarDisponibilidad_sinFechaInicio_encuentraHuecoEnLaJornadaAunqueElOrigenSeaMedianoche() {
        Planificacion planificacion = planificacion(null);
        Actividad actividad = actividad(1L, "A", 120, planificacion);

        // Sólo el lunes 26 se trabaja: el rango estricto [ES, LF] caería el
        // domingo a la madrugada y no tendría hueco. El rango provisional debe
        // llegar hasta la entrega y encontrar el lunes.
        when(jornadaAgendaRepository.findByAgenda_IdAgendaOrderByDiaSemana(10L))
                .thenReturn(List.of(jornadaCorrida(1, LocalTime.of(9, 0), LocalTime.of(18, 0))));
        when(actividadRepository.findConPredecesorasByPlanificacion(100L))
                .thenReturn(List.of(actividad));

        ResultadoDisponibilidad resultado = calendarioService.verificarDisponibilidad(1L, actividad);

        assertThat(resultado.provisional()).isTrue();
        assertThat(resultado.disponible()).isTrue();
    }

    @Test
    void verificarDisponibilidad_sinJornada_devuelveNoDisponible() {
        Planificacion planificacion = planificacion(INICIO);
        Actividad actividad = actividad(1L, "A", 120, planificacion);

        when(jornadaAgendaRepository.findByAgenda_IdAgendaOrderByDiaSemana(10L))
                .thenReturn(List.of());
        when(actividadRepository.findConPredecesorasByPlanificacion(100L))
                .thenReturn(List.of(actividad));

        ResultadoDisponibilidad resultado = calendarioService.verificarDisponibilidad(1L, actividad);

        assertThat(resultado.disponible()).isFalse();
    }

    @Test
    void verificarDisponibilidadMiembros_requiereHuecoComun() {
        Planificacion planificacion = planificacion(INICIO);
        Actividad actividad = actividad(1L, "A", 60, planificacion);
        Usuario segundoUsuario = Usuario.builder()
                .idUsuario(2L)
                .estado(EstadoUsuario.Activo)
                .build();
        Agenda segundaAgenda = Agenda.builder()
                .idAgenda(20L)
                .margenActividadMinutos(30)
                .build();

        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(segundoUsuario));
        when(agendaRepository.findByUsuario_IdUsuario(2L))
                .thenReturn(Optional.of(segundaAgenda));
        when(jornadaAgendaRepository.findByAgenda_IdAgendaOrderByDiaSemana(10L))
                .thenReturn(List.of(jornadaCorrida(1, LocalTime.of(9, 0), LocalTime.of(12, 0))));
        when(jornadaAgendaRepository.findByAgenda_IdAgendaOrderByDiaSemana(20L))
                .thenReturn(List.of(jornadaCorrida(1, LocalTime.of(10, 0), LocalTime.of(13, 0))));
        when(actividadRepository.findConPredecesorasByPlanificacion(100L))
                .thenReturn(List.of(actividad));

        ResultadoDisponibilidad resultado = calendarioService.verificarDisponibilidadMiembros(
                List.of(1L, 2L), actividad);

        assertThat(resultado.disponible()).isTrue();
        assertThat(resultado.inicioSeleccionado()).isEqualTo(INICIO.plusMinutes(90));
        assertThat(resultado.finSeleccionado()).isEqualTo(INICIO.plusMinutes(150));
        assertThat(resultado.miembros()).hasSize(2);
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

    private Actividad actividad(Long id, String nombre, int duracion, Planificacion planificacion) {
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

    private JornadaAgenda jornadaPartida(
            int dia,
            LocalTime inicioManana,
            LocalTime finManana,
            LocalTime inicioTarde,
            LocalTime finTarde) {

        return JornadaAgenda.builder()
                .idJornada((long) dia)
                .agenda(agenda())
                .diaSemana(dia)
                .horaInicioManana(inicioManana)
                .horaFinManana(finManana)
                .horaInicioTarde(inicioTarde)
                .horaFinTarde(finTarde)
                .build();
    }
}
