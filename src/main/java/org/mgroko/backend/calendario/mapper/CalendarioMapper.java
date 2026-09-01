package org.mgroko.backend.calendario.mapper;

import java.util.List;

import org.mgroko.backend.calendario.dto.BloqueoActividadResponse;
import org.mgroko.backend.calendario.dto.BloqueoResponse;
import org.mgroko.backend.calendario.dto.ConfigJornadaResponse;
import org.mgroko.backend.calendario.dto.JornadaDiaResponse;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Agenda;
import org.mgroko.backend.modelo.BloqueoAgenda;
import org.mgroko.backend.modelo.JornadaAgenda;

public class CalendarioMapper {

    private CalendarioMapper() {}

    public static JornadaDiaResponse toJornadaDiaResponse(JornadaAgenda jornada) {
        return new JornadaDiaResponse(
                jornada.getDiaSemana(),
                jornada.getHoraInicio(),
                jornada.getHoraFin());
    }

    public static ConfigJornadaResponse toConfigJornadaResponse(Agenda agenda, List<JornadaAgenda> dias) {
        List<JornadaDiaResponse> jornadas = dias.stream()
                .map(CalendarioMapper::toJornadaDiaResponse)
                .toList();
        return new ConfigJornadaResponse(agenda.getMargenActividadMinutos(), jornadas);
    }

    public static BloqueoResponse toBloqueoResponse(BloqueoAgenda bloqueo) {
        return new BloqueoResponse(
                bloqueo.getIdBloqueo(),
                bloqueo.getFechaHoraInicio(),
                bloqueo.getFechaHoraFin(),
                bloqueo.getMotivo());
    }

    /**
     * Convierte una actividad en el bloqueo calculado correspondiente,
     * extendiendo el rango con el margen por actividad (buffer) a cada lado.
     */
    public static BloqueoActividadResponse toBloqueoActividadResponse(Actividad actividad, int margenMinutos) {
        return new BloqueoActividadResponse(
                actividad.getIdActividad(),
                actividad.getNombre(),
                actividad.getFechaHoraInicio().minusMinutes(margenMinutos),
                actividad.getFechaHoraFin().plusMinutes(margenMinutos));
    }
}