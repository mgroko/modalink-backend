package org.mgroko.backend.calendario.mapper;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.mgroko.backend.calendario.dto.BloqueoActividadResponse;
import org.mgroko.backend.calendario.dto.BloqueoResponse;
import org.mgroko.backend.calendario.dto.ConfigJornadaResponse;
import org.mgroko.backend.calendario.dto.JornadaDiaResponse;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Agenda;
import org.mgroko.backend.modelo.BloqueoAgenda;
import org.mgroko.backend.modelo.JornadaAgenda;

class CalendarioMapperTest {

    @Test
    void toJornadaDiaResponse_mapeaCampos() {
        JornadaAgenda jornada = JornadaAgenda.builder()
                .idJornada(5L)
                .diaSemana(3)
                .horaInicio(LocalTime.of(10, 0))
                .horaFin(LocalTime.of(20, 0))
                .build();

        JornadaDiaResponse response = CalendarioMapper.toJornadaDiaResponse(jornada);

        assertEquals(3, response.diaSemana());
        assertEquals(LocalTime.of(10, 0), response.horaInicio());
        assertEquals(LocalTime.of(20, 0), response.horaFin());
    }

    @Test
    void toConfigJornadaResponse_mapeaMargenYLista() {
        Agenda agenda = Agenda.builder().idAgenda(1L).margenActividadMinutos(90).build();
        List<JornadaAgenda> dias = List.of(
                JornadaAgenda.builder().diaSemana(1)
                        .horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(18, 0)).build(),
                JornadaAgenda.builder().diaSemana(2)
                        .horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(18, 0)).build());

        ConfigJornadaResponse response = CalendarioMapper.toConfigJornadaResponse(agenda, dias);

        assertEquals(90, response.margenActividadMinutos());
        assertEquals(2, response.dias().size());
        assertEquals(1, response.dias().get(0).diaSemana());
        assertEquals(2, response.dias().get(1).diaSemana());
    }

    @Test
    void toBloqueoResponse_mapeaCampos() {
        BloqueoAgenda bloqueo = BloqueoAgenda.builder()
                .idBloqueo(7L)
                .fechaHoraInicio(LocalDateTime.of(2026, 9, 15, 10, 0))
                .fechaHoraFin(LocalDateTime.of(2026, 9, 15, 14, 0))
                .motivo("Dentista")
                .build();

        BloqueoResponse response = CalendarioMapper.toBloqueoResponse(bloqueo);

        assertEquals(7L, response.idBloqueo());
        assertEquals(LocalDateTime.of(2026, 9, 15, 10, 0), response.fechaHoraInicio());
        assertEquals(LocalDateTime.of(2026, 9, 15, 14, 0), response.fechaHoraFin());
        assertEquals("Dentista", response.motivo());
    }

    @Test
    void toBloqueoActividadResponse_aplicaMargenALosDosExtremos() {
        Actividad actividad = Actividad.builder()
                .idActividad(11L)
                .nombre("Sesión de fotos")
                .fechaHoraInicio(LocalDateTime.of(2026, 9, 10, 10, 0))
                .fechaHoraFin(LocalDateTime.of(2026, 9, 10, 12, 0))
                .build();

        BloqueoActividadResponse response = CalendarioMapper.toBloqueoActividadResponse(actividad, 60);

        assertEquals(11L, response.idActividad());
        assertEquals("Sesión de fotos", response.nombre());
        assertEquals(LocalDateTime.of(2026, 9, 10, 9, 0), response.fechaHoraInicio());
        assertEquals(LocalDateTime.of(2026, 9, 10, 13, 0), response.fechaHoraFin());
    }
}