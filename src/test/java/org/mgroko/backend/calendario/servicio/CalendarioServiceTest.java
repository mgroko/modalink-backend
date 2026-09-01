package org.mgroko.backend.calendario.servicio;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mgroko.backend.calendario.dto.BloqueoResponse;
import org.mgroko.backend.calendario.dto.CalendarioResponse;
import org.mgroko.backend.calendario.dto.ConfigJornadaRequest;
import org.mgroko.backend.calendario.dto.ConfigJornadaResponse;
import org.mgroko.backend.calendario.dto.JornadaDiaRequest;
import org.mgroko.backend.calendario.dto.MarcarNoDisponibleRequest;
import org.mgroko.backend.calendario.exception.AgendaNoEncontradaException;
import org.mgroko.backend.calendario.exception.BloqueoNoEncontradoException;
import org.mgroko.backend.calendario.exception.BloqueoSolapadoException;
import org.mgroko.backend.calendario.exception.HorarioComprometidoException;
import org.mgroko.backend.calendario.exception.JornadaInvalidaException;
import org.mgroko.backend.calendario.exception.RangoInvalidoException;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Agenda;
import org.mgroko.backend.modelo.BloqueoAgenda;
import org.mgroko.backend.modelo.JornadaAgenda;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.repositorio.ActividadRepository;
import org.mgroko.backend.repositorio.AgendaRepository;
import org.mgroko.backend.repositorio.BloqueoAgendaRepository;
import org.mgroko.backend.repositorio.JornadaAgendaRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CalendarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AgendaRepository agendaRepository;

    @Mock
    private JornadaAgendaRepository jornadaAgendaRepository;

    @Mock
    private BloqueoAgendaRepository bloqueoAgendaRepository;

    @Mock
    private ActividadRepository actividadRepository;

    @InjectMocks
    private CalendarioService calendarioService;

    private void mockUsuarioActivo() {
        Usuario usuario = Usuario.builder()
                .idUsuario(1L).nombre("Juan").apellido("Perez").estado(EstadoUsuario.Activo).build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
    }

    private void mockAgenda() {
        Agenda agenda = Agenda.builder().idAgenda(10L).margenActividadMinutos(60).build();
        when(agendaRepository.findByUsuario_IdUsuario(1L)).thenReturn(Optional.of(agenda));
    }

    private void mockSinActividades() {
        when(actividadRepository.findActividadesDeUsuario(anyLong(), anyCollection())).thenReturn(List.of());
    }

    private Actividad actividad(LocalDateTime inicio, LocalDateTime fin) {
        return Actividad.builder()
                .idActividad(5L).nombre("Sesión")
                .fechaHoraInicio(inicio).fechaHoraFin(fin).build();
    }

    // ------------------------------------------------------------------
    // obtener
    // ------------------------------------------------------------------

    @Test
    void obtener_devuelveJornadaBloqueosYActividades() {
        mockUsuarioActivo();
        mockAgenda();
        when(jornadaAgendaRepository.findByAgenda_IdAgendaOrderByDiaSemana(10L))
                .thenReturn(List.of(JornadaAgenda.builder().diaSemana(1)
                        .horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(18, 0)).build()));
        when(bloqueoAgendaRepository.findByAgenda_IdAgendaOrderByFechaHoraInicio(10L))
                .thenReturn(List.of(BloqueoAgenda.builder().idBloqueo(1L)
                        .fechaHoraInicio(LocalDateTime.of(2026, 9, 15, 10, 0))
                        .fechaHoraFin(LocalDateTime.of(2026, 9, 15, 14, 0)).build()));
        when(actividadRepository.findActividadesDeUsuario(anyLong(), anyCollection()))
                .thenReturn(List.of(actividad(
                        LocalDateTime.of(2026, 9, 10, 10, 0),
                        LocalDateTime.of(2026, 9, 10, 12, 0))));

        CalendarioResponse response = calendarioService.obtener(1L);

        assertEquals(60, response.jornada().margenActividadMinutos());
        assertEquals(1, response.jornada().dias().size());
        assertEquals(1, response.bloqueosManuales().size());
        assertEquals(1, response.actividades().size());
        assertEquals(LocalDateTime.of(2026, 9, 10, 9, 0), response.actividades().get(0).fechaHoraInicio());
        assertEquals(LocalDateTime.of(2026, 9, 10, 13, 0), response.actividades().get(0).fechaHoraFin());
    }

    @Test
    void obtener_sinAgenda_lanzaAgendaNoEncontrada() {
        mockUsuarioActivo();
        when(agendaRepository.findByUsuario_IdUsuario(1L)).thenReturn(Optional.empty());

        assertThrows(AgendaNoEncontradaException.class, () -> calendarioService.obtener(1L));
    }

    // ------------------------------------------------------------------
    // configurarJornada
    // ------------------------------------------------------------------

    @Test
    void configurarJornada_sinDiasExistentes_insertaTodos() {
        mockUsuarioActivo();
        mockAgenda();
        ConfigJornadaRequest request = new ConfigJornadaRequest(90, List.of(
                new JornadaDiaRequest(1, LocalTime.of(9, 0), LocalTime.of(18, 0)),
                new JornadaDiaRequest(3, LocalTime.of(10, 0), LocalTime.of(20, 0))));
        when(jornadaAgendaRepository.findByAgenda_IdAgendaOrderByDiaSemana(10L)).thenReturn(List.of());
        when(jornadaAgendaRepository.save(any(JornadaAgenda.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(agendaRepository.save(any(Agenda.class))).thenAnswer(inv -> inv.getArgument(0));

        ConfigJornadaResponse response = calendarioService.configurarJornada(1L, request);

        verify(jornadaAgendaRepository, times(2)).save(any(JornadaAgenda.class));
        verify(jornadaAgendaRepository).deleteAll(anyCollection());
        verify(agendaRepository).save(any(Agenda.class));
        assertEquals(90, response.margenActividadMinutos());
    }

    @Test
    void configurarJornada_actualizaDiaExistente_sinReinsertar() {
        mockUsuarioActivo();
        mockAgenda();
        JornadaAgenda lunes = JornadaAgenda.builder().idJornada(1L).diaSemana(1)
                .horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(18, 0)).build();
        when(jornadaAgendaRepository.findByAgenda_IdAgendaOrderByDiaSemana(10L))
                .thenReturn(List.of(lunes));
        when(agendaRepository.save(any(Agenda.class))).thenAnswer(inv -> inv.getArgument(0));

        ConfigJornadaResponse response = calendarioService.configurarJornada(1L,
                new ConfigJornadaRequest(60, List.of(
                        new JornadaDiaRequest(1, LocalTime.of(10, 0), LocalTime.of(19, 0)))));

        assertEquals(LocalTime.of(10, 0), lunes.getHoraInicio());
        assertEquals(LocalTime.of(19, 0), lunes.getHoraFin());
        assertEquals(LocalTime.of(10, 0), response.dias().get(0).horaInicio());
        verify(jornadaAgendaRepository, never()).save(any(JornadaAgenda.class));
    }

    @Test
    void configurarJornada_eliminaDiasNoEnviados() {
        mockUsuarioActivo();
        mockAgenda();
        JornadaAgenda lunes = JornadaAgenda.builder().idJornada(1L).diaSemana(1)
                .horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(18, 0)).build();
        JornadaAgenda martes = JornadaAgenda.builder().idJornada(2L).diaSemana(2)
                .horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(18, 0)).build();
        when(jornadaAgendaRepository.findByAgenda_IdAgendaOrderByDiaSemana(10L))
                .thenReturn(List.of(lunes, martes));
        when(agendaRepository.save(any(Agenda.class))).thenAnswer(inv -> inv.getArgument(0));

        calendarioService.configurarJornada(1L,
                new ConfigJornadaRequest(60, List.of(
                        new JornadaDiaRequest(1, LocalTime.of(9, 0), LocalTime.of(18, 0)))));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<JornadaAgenda>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(jornadaAgendaRepository).deleteAll(captor.capture());
        assertEquals(List.of(martes), new ArrayList<>(captor.getValue()));
        verify(jornadaAgendaRepository, never()).save(any(JornadaAgenda.class));
    }

    @Test
    void configurarJornada_mismosDias_noReescribe() {
        mockUsuarioActivo();
        mockAgenda();
        JornadaAgenda lunes = JornadaAgenda.builder().idJornada(1L).diaSemana(1)
                .horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(18, 0)).build();
        when(jornadaAgendaRepository.findByAgenda_IdAgendaOrderByDiaSemana(10L))
                .thenReturn(List.of(lunes));
        when(agendaRepository.save(any(Agenda.class))).thenAnswer(inv -> inv.getArgument(0));

        ConfigJornadaResponse response = calendarioService.configurarJornada(1L,
                new ConfigJornadaRequest(60, List.of(
                        new JornadaDiaRequest(1, LocalTime.of(9, 0), LocalTime.of(18, 0)))));

        verify(jornadaAgendaRepository, never()).save(any(JornadaAgenda.class));
        assertEquals(1, response.dias().size());
        assertEquals(1, response.dias().get(0).diaSemana());
    }

    @Test
    void configurarJornada_diaFueraDeRango_lanzaJornadaInvalida() {
        mockUsuarioActivo();
        ConfigJornadaRequest request = new ConfigJornadaRequest(60, List.of(
                new JornadaDiaRequest(8, LocalTime.of(9, 0), LocalTime.of(18, 0))));

        assertThrows(JornadaInvalidaException.class,
                () -> calendarioService.configurarJornada(1L, request));
    }

    @Test
    void configurarJornada_horaFinAntesDeInicio_lanzaJornadaInvalida() {
        mockUsuarioActivo();
        ConfigJornadaRequest request = new ConfigJornadaRequest(60, List.of(
                new JornadaDiaRequest(1, LocalTime.of(18, 0), LocalTime.of(9, 0))));

        assertThrows(JornadaInvalidaException.class,
                () -> calendarioService.configurarJornada(1L, request));
    }

    @Test
    void configurarJornada_diaDuplicado_lanzaJornadaInvalida() {
        mockUsuarioActivo();
        ConfigJornadaRequest request = new ConfigJornadaRequest(60, List.of(
                new JornadaDiaRequest(1, LocalTime.of(9, 0), LocalTime.of(18, 0)),
                new JornadaDiaRequest(1, LocalTime.of(10, 0), LocalTime.of(20, 0))));

        assertThrows(JornadaInvalidaException.class,
                () -> calendarioService.configurarJornada(1L, request));
    }

    // ------------------------------------------------------------------
    // marcarNoDisponible (UC-18)
    // ------------------------------------------------------------------

    @Test
    void marcarNoDisponible_valido_creaBloqueo() {
        mockUsuarioActivo();
        mockAgenda();
        mockSinActividades();
        when(bloqueoAgendaRepository.existsByAgenda_IdAgendaAndFechaHoraInicioLessThanAndFechaHoraFinGreaterThan(
                10L, LocalDateTime.of(2026, 9, 15, 14, 0), LocalDateTime.of(2026, 9, 15, 10, 0)))
                .thenReturn(false);
        when(bloqueoAgendaRepository.save(any(BloqueoAgenda.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        BloqueoResponse response = calendarioService.marcarNoDisponible(1L,
                new MarcarNoDisponibleRequest(
                        LocalDateTime.of(2026, 9, 15, 10, 0),
                        LocalDateTime.of(2026, 9, 15, 14, 0),
                        "Compromiso"));

        assertEquals(LocalDateTime.of(2026, 9, 15, 10, 0), response.fechaHoraInicio());
        assertEquals("Compromiso", response.motivo());
        verify(bloqueoAgendaRepository).save(any(BloqueoAgenda.class));
    }

    @Test
    void marcarNoDisponible_finAntesDeInicio_lanzaRangoInvalido() {
        mockUsuarioActivo();
        MarcarNoDisponibleRequest request = new MarcarNoDisponibleRequest(
                LocalDateTime.of(2026, 9, 15, 14, 0),
                LocalDateTime.of(2026, 9, 15, 10, 0),
                null);

        assertThrows(RangoInvalidoException.class,
                () -> calendarioService.marcarNoDisponible(1L, request));
    }

    @Test
    void marcarNoDisponible_solapaActividad_lanzaHorarioComprometido() {
        mockUsuarioActivo();
        mockAgenda();
        when(actividadRepository.findActividadesDeUsuario(anyLong(), anyCollection()))
                .thenReturn(List.of(actividad(
                        LocalDateTime.of(2026, 9, 15, 10, 0),
                        LocalDateTime.of(2026, 9, 15, 12, 0))));

        assertThrows(HorarioComprometidoException.class,
                () -> calendarioService.marcarNoDisponible(1L, new MarcarNoDisponibleRequest(
                        LocalDateTime.of(2026, 9, 15, 10, 30),
                        LocalDateTime.of(2026, 9, 15, 11, 30), null)));

        verify(bloqueoAgendaRepository, never()).save(any());
    }

    @Test
    void marcarNoDisponible_solapaActividadSoloPorMargen_lanzaHorarioComprometido() {
        mockUsuarioActivo();
        mockAgenda();
        // Actividad 10:00-12:00, margen 60 -> ocupa 09:00-13:00.
        when(actividadRepository.findActividadesDeUsuario(anyLong(), anyCollection()))
                .thenReturn(List.of(actividad(
                        LocalDateTime.of(2026, 9, 15, 10, 0),
                        LocalDateTime.of(2026, 9, 15, 12, 0))));

        assertThrows(HorarioComprometidoException.class,
                () -> calendarioService.marcarNoDisponible(1L, new MarcarNoDisponibleRequest(
                        LocalDateTime.of(2026, 9, 15, 8, 30),
                        LocalDateTime.of(2026, 9, 15, 9, 30), null)));

        verify(bloqueoAgendaRepository, never()).save(any());
    }

    @Test
    void marcarNoDisponible_solapaBloqueoManual_lanzaBloqueoSolapado() {
        mockUsuarioActivo();
        mockAgenda();
        mockSinActividades();
        when(bloqueoAgendaRepository.existsByAgenda_IdAgendaAndFechaHoraInicioLessThanAndFechaHoraFinGreaterThan(
                10L, LocalDateTime.of(2026, 9, 15, 16, 0), LocalDateTime.of(2026, 9, 15, 12, 0)))
                .thenReturn(true);

        assertThrows(BloqueoSolapadoException.class,
                () -> calendarioService.marcarNoDisponible(1L, new MarcarNoDisponibleRequest(
                        LocalDateTime.of(2026, 9, 15, 12, 0),
                        LocalDateTime.of(2026, 9, 15, 16, 0), null)));

        verify(bloqueoAgendaRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // marcarDisponible (UC-17)
    // ------------------------------------------------------------------

    @Test
    void marcarDisponible_valido_eliminaBloqueo() {
        mockUsuarioActivo();
        mockAgenda();
        BloqueoAgenda bloqueo = BloqueoAgenda.builder().idBloqueo(3L)
                .fechaHoraInicio(LocalDateTime.of(2026, 9, 15, 10, 0))
                .fechaHoraFin(LocalDateTime.of(2026, 9, 15, 14, 0)).build();
        when(bloqueoAgendaRepository.findByIdBloqueoAndAgenda_IdAgenda(3L, 10L))
                .thenReturn(Optional.of(bloqueo));
        mockSinActividades();

        calendarioService.marcarDisponible(1L, 3L);

        verify(bloqueoAgendaRepository).delete(bloqueo);
    }

    @Test
    void marcarDisponible_bloqueoInexistente_lanzaBloqueoNoEncontrado() {
        mockUsuarioActivo();
        mockAgenda();
        when(bloqueoAgendaRepository.findByIdBloqueoAndAgenda_IdAgenda(3L, 10L))
                .thenReturn(Optional.empty());

        assertThrows(BloqueoNoEncontradoException.class,
                () -> calendarioService.marcarDisponible(1L, 3L));

        verify(bloqueoAgendaRepository, never()).delete(any());
    }

    @Test
    void marcarDisponible_solapaActividad_lanzaHorarioComprometido() {
        mockUsuarioActivo();
        mockAgenda();
        BloqueoAgenda bloqueo = BloqueoAgenda.builder().idBloqueo(3L)
                .fechaHoraInicio(LocalDateTime.of(2026, 9, 15, 10, 0))
                .fechaHoraFin(LocalDateTime.of(2026, 9, 15, 14, 0)).build();
        when(bloqueoAgendaRepository.findByIdBloqueoAndAgenda_IdAgenda(3L, 10L))
                .thenReturn(Optional.of(bloqueo));
        when(actividadRepository.findActividadesDeUsuario(anyLong(), anyCollection()))
                .thenReturn(List.of(actividad(
                        LocalDateTime.of(2026, 9, 15, 11, 0),
                        LocalDateTime.of(2026, 9, 15, 12, 0))));

        assertThrows(HorarioComprometidoException.class,
                () -> calendarioService.marcarDisponible(1L, 3L));

        verify(bloqueoAgendaRepository, never()).delete(any());
    }
}