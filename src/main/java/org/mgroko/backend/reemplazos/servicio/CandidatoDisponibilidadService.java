package org.mgroko.backend.reemplazos.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.calendario.exception.AgendaNoEncontradaException;
import org.mgroko.backend.calendario.servicio.CalendarioService;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.proyectos.exception.ProyectoNoConfirmableException;
import org.mgroko.backend.proyectos.servicio.ReprogramarProyectoService;
import org.mgroko.backend.reemplazos.enums.TipoDisponibilidad;
import org.mgroko.backend.reemplazos.servicio.modelo.DisponibilidadCandidato;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Evalúa la disponibilidad de un candidato para una actividad.
 *
 * <p>Primero prueba la ventana exacta ya planificada. Si la actividad está
 * {@code PENDIENTE}, además simula la asignación para saber si podría entrar
 * reprogramando la actividad. Las actividades {@code EN_CURSO} no se
 * reprograman.</p>
 */
@Service
public class CandidatoDisponibilidadService {

    private static final Logger LOG =
            LoggerFactory.getLogger(CandidatoDisponibilidadService.class);

    private final CalendarioService calendarioService;
    private final ReprogramarProyectoService reprogramarProyectoService;
    private final Clock reloj;

    public CandidatoDisponibilidadService(
            CalendarioService calendarioService,
            ReprogramarProyectoService reprogramarProyectoService,
            Clock reloj) {
        this.calendarioService = calendarioService;
        this.reprogramarProyectoService = reprogramarProyectoService;
        this.reloj = reloj;
    }

    public DisponibilidadCandidato evaluar(
            Proyecto proyecto,
            Actividad actividad,
            Long idUsuario) {

        if (proyecto == null || actividad == null || idUsuario == null) {
            return DisponibilidadCandidato.noDisponible();
        }

        LocalDateTime inicio = actividad.getFechaHoraInicio();
        LocalDateTime fin = actividad.getFechaHoraFin();
        if (inicio == null || fin == null
                || !fin.isAfter(LocalDateTime.now(reloj))) {
            return DisponibilidadCandidato.noDisponible();
        }

        try {
            if (calendarioService.ventanaDisponiblePara(
                    List.of(idUsuario),
                    inicio,
                    fin,
                    actividad.getIdActividad())) {
                return new DisponibilidadCandidato(
                        TipoDisponibilidad.EXACTA,
                        inicio,
                        fin);
            }
        } catch (AgendaNoEncontradaException ex) {
            return DisponibilidadCandidato.noDisponible();
        }

        if (actividad.getEstado() != EstadoActividad.PENDIENTE) {
            return DisponibilidadCandidato.noDisponible();
        }

        try {
            var simulacion = reprogramarProyectoService.simularAsignacion(
                    proyecto.getIdProyecto(),
                    actividad.getIdActividad(),
                    List.of(idUsuario));

            boolean disponible = simulacion.disponibilidad() != null
                    && simulacion.disponibilidad().disponible();
            if (!disponible && simulacion.inicioPropuesto() == null) {
                return DisponibilidadCandidato.noDisponible();
            }

            return new DisponibilidadCandidato(
                    TipoDisponibilidad.CON_REPROGRAMACION,
                    simulacion.inicioPropuesto(),
                    simulacion.finPropuesto());
        } catch (ProyectoNoConfirmableException ex) {
            return DisponibilidadCandidato.noDisponible();
        } catch (RuntimeException ex) {
            LOG.warn("No se pudo simular la asignación del usuario {} a la actividad {}.",
                    idUsuario, actividad.getIdActividad(), ex);
            return DisponibilidadCandidato.noDisponible();
        }
    }
}
