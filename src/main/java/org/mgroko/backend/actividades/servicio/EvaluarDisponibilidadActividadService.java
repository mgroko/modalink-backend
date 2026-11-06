package org.mgroko.backend.actividades.servicio;

import java.util.List;

import org.mgroko.backend.calendario.dto.ResultadoDisponibilidad;
import org.mgroko.backend.calendario.servicio.CalendarioService;
import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.enums.EstadoActividad;
import org.mgroko.backend.proyectos.exception.ProyectoNoConfirmableException;
import org.mgroko.backend.proyectos.servicio.ReprogramarProyectoService;
import org.springframework.stereotype.Service;

/**
 * Punto único para evaluar la disponibilidad de una actividad.
 *
 * <p>Para proyectos publicados o confirmados con actividad {@code PENDIENTE}
 * simula la asignación completa: la actividad se mueve a la primera ventana
 * común de los usuarios indicados y se devuelve la propuesta. Para el resto de
 * los casos usa la verificación clásica de calendario.</p>
 *
 * <p>La simulación trabaja sobre copias y no persiste nada. Si el planner
 * determina que no hay ventana, devuelve un resultado con {@code disponible =
 * false} y {@code codigoMotivo}, en lugar de propagar una excepción genérica.</p>
 */
@Service
public class EvaluarDisponibilidadActividadService {

    private final CalendarioService calendarioService;
    private final ReprogramarProyectoService reprogramarProyectoService;

    public EvaluarDisponibilidadActividadService(
            CalendarioService calendarioService,
            ReprogramarProyectoService reprogramarProyectoService) {
        this.calendarioService = calendarioService;
        this.reprogramarProyectoService = reprogramarProyectoService;
    }

    public ResultadoDisponibilidad evaluar(
            Proyecto proyecto,
            Actividad actividad,
            List<Long> idsUsuarios) {

        if (proyecto != null
                && proyecto.getEstado() != null
                && proyecto.getEstado().permiteReprogramacion()
                && actividad != null
                && actividad.getEstado() == EstadoActividad.PENDIENTE) {
            try {
                ReprogramarProyectoService.SimulacionAsignacion simulacion =
                        reprogramarProyectoService.simularAsignacion(
                                proyecto.getIdProyecto(),
                                actividad.getIdActividad(),
                                idsUsuarios);
                if (simulacion.disponibilidad() != null) {
                    return simulacion.disponibilidad()
                            .conProvisional(proyecto.getFechaInicio() == null);
                }
            } catch (ProyectoNoConfirmableException ex) {
                return resultadoNoDisponible(ex, proyecto);
            }
        }

        return calendarioService.verificarDisponibilidadMiembros(
                idsUsuarios,
                actividad);
    }

    private ResultadoDisponibilidad resultadoNoDisponible(
            ProyectoNoConfirmableException ex,
            Proyecto proyecto) {

        return new ResultadoDisponibilidad(
                false,
                proyecto.getFechaInicio() == null,
                null,
                null,
                null,
                null,
                ex.getMessage(),
                List.of(),
                null,
                null,
                null,
                List.of(),
                ex.getCodigo(),
                List.of());
    }
}
