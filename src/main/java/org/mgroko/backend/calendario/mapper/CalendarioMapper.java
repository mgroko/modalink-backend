package org.mgroko.backend.calendario.mapper;

import java.util.List;
import java.util.Objects;

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
                jornada.getHoraInicioManana(),
                jornada.getHoraFinManana(),
                jornada.getHoraInicioTarde(),
                jornada.getHoraFinTarde());
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
     * Mapea un bloqueo manual ocultando el motivo para visualización pública (terceros).
     */
    public static BloqueoResponse toBloqueoResponseAnonimizado(BloqueoAgenda bloqueo) {
        return new BloqueoResponse(
                bloqueo.getIdBloqueo(),
                bloqueo.getFechaHoraInicio(),
                bloqueo.getFechaHoraFin(),
                null);
    }

    /**
     * Convierte una actividad en el bloqueo calculado correspondiente,
     * extendiendo el rango con el margen por actividad (buffer) a cada lado.
     * Falla rápido si la actividad no trae inicio o fin (columnas NOT NULL
     * en BD; solo posible en objetos transient o mal construidos).
     */
    public static BloqueoActividadResponse toBloqueoActividadResponse(Actividad actividad, int margenMinutos) {
        Objects.requireNonNull(actividad.getFechaHoraInicio(),
                "La actividad (id=" + actividad.getIdActividad() + ") no tiene fechaHoraInicio");
        Objects.requireNonNull(actividad.getFechaHoraFin(),
                "La actividad (id=" + actividad.getIdActividad() + ") no tiene fechaHoraFin");
        return new BloqueoActividadResponse(
                actividad.getIdActividad(),
                actividad.getNombre(),
                actividad.getFechaHoraInicio().minusMinutes(margenMinutos),
                actividad.getFechaHoraFin().plusMinutes(margenMinutos));
    }
}