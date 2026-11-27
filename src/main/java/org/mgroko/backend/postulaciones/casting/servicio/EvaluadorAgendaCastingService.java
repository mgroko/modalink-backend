package org.mgroko.backend.postulaciones.casting.servicio;

import java.util.List;

import org.mgroko.backend.calendario.exception.AgendaNoEncontradaException;
import org.mgroko.backend.calendario.servicio.CalendarioService;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.postulaciones.casting.dto.AgendaCastingResponse;
import org.mgroko.backend.postulaciones.casting.dto.RequerimientoCastingResponse;
import org.mgroko.backend.postulaciones.dto.TipoPostulacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Evalúa, solo para requerimientos de actividad, si el perfil tiene la
 * ventana exacta de la actividad disponible en su agenda.
 *
 * <p>No simula reprogramaciones: el tablero es una foto de la disponibilidad
 * actual y la aceptación real vuelve a validar el hueco común.</p>
 */
@Service
public class EvaluadorAgendaCastingService {

    private static final Logger LOG =
            LoggerFactory.getLogger(EvaluadorAgendaCastingService.class);

    private final CalendarioService calendarioService;

    public EvaluadorAgendaCastingService(CalendarioService calendarioService) {
        this.calendarioService = calendarioService;
    }

    public AgendaCastingResponse evaluar(
            RequerimientoCastingResponse requerimiento,
            Perfil perfil) {

        if (requerimiento == null
                || requerimiento.tipo() != TipoPostulacion.ACTIVIDAD) {
            return AgendaCastingResponse.noAplica();
        }

        if (requerimiento.fechaHoraInicio() == null
                || requerimiento.fechaHoraFin() == null
                || !requerimiento.fechaHoraFin().isAfter(requerimiento.fechaHoraInicio())) {
            return AgendaCastingResponse.sinEvaluar("ACTIVIDAD_SIN_FECHA");
        }

        if (perfil == null
                || perfil.getUsuario() == null
                || perfil.getUsuario().getIdUsuario() == null) {
            return AgendaCastingResponse.sinEvaluar("PERFIL_SIN_USUARIO");
        }

        try {
            boolean disponible = calendarioService.ventanaDisponiblePara(
                    List.of(perfil.getUsuario().getIdUsuario()),
                    requerimiento.fechaHoraInicio(),
                    requerimiento.fechaHoraFin(),
                    requerimiento.idActividad());

            return new AgendaCastingResponse(
                    true,
                    disponible,
                    disponible ? "DISPONIBLE" : "NO_DISPONIBLE",
                    requerimiento.fechaHoraInicio(),
                    requerimiento.fechaHoraFin(),
                    disponible ? null : "SIN_HUECO");
        } catch (AgendaNoEncontradaException ex) {
            return new AgendaCastingResponse(
                    true,
                    false,
                    "NO_DISPONIBLE",
                    requerimiento.fechaHoraInicio(),
                    requerimiento.fechaHoraFin(),
                    "SIN_AGENDA");
        } catch (RuntimeException ex) {
            LOG.warn("No se pudo evaluar la agenda del perfil {} para la actividad {}.",
                    perfil.getIdPerfil(), requerimiento.idActividad(), ex);
            return new AgendaCastingResponse(
                    false,
                    null,
                    "SIN_EVALUAR",
                    requerimiento.fechaHoraInicio(),
                    requerimiento.fechaHoraFin(),
                    "ERROR_EVALUACION");
        }
    }
}
