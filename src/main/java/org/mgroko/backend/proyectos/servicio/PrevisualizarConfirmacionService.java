package org.mgroko.backend.proyectos.servicio;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.proyectos.dto.ActividadProgramadaResponse;
import org.mgroko.backend.proyectos.dto.BloqueoConfirmacionResponse;
import org.mgroko.backend.proyectos.dto.PrevisualizacionConfirmacionResponse;
import org.mgroko.backend.proyectos.exception.ProyectoNoConfirmableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Previsualización de UC-60. Ejecuta las mismas validaciones que la
 * confirmación y, si el proyecto está en condiciones, hace un dry-run del
 * planificador sin persistir nada.
 */
@Service
public class PrevisualizarConfirmacionService {

    private final ContextoConfirmacionProyectoService contextoService;
    private final PlanificadorRecursosService planificadorRecursosService;

    public PrevisualizarConfirmacionService(
            ContextoConfirmacionProyectoService contextoService,
            PlanificadorRecursosService planificadorRecursosService) {
        this.contextoService = contextoService;
        this.planificadorRecursosService = planificadorRecursosService;
    }

    @Transactional(readOnly = true)
    public PrevisualizacionConfirmacionResponse previsualizar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto) {

        ContextoConfirmacionProyectoService.Contexto contexto =
                contextoService.cargar(idUsuario, idPerfilActivo, idProyecto, false);
        LocalDateTime ahora = contexto.fechaInicioPropuesta();

        List<BloqueoConfirmacionResponse> bloqueos = new ArrayList<>(contexto.bloqueos());
        List<ActividadProgramadaResponse> propuesta = List.of();

        if (bloqueos.isEmpty()) {
            try {
                PlanificadorRecursosService.Resultado plan =
                        planificadorRecursosService.planificar(
                                contexto.planificacion(),
                                contexto.actividades(),
                                ahora,
                                false);

                List<ActividadProgramadaResponse> horarios = new ArrayList<>();
                for (Actividad actividad : contexto.actividadesNoCanceladas()) {
                    LocalDateTime inicio = plan.inicios().get(actividad.getIdActividad());
                    if (inicio == null) {
                        continue;
                    }
                    horarios.add(new ActividadProgramadaResponse(
                            actividad.getIdActividad(),
                            actividad.getNombre(),
                            inicio,
                            inicio.plusMinutes(actividad.getDuracionMinutos()),
                            plan.calculo().nodoDe(actividad.getIdActividad()).fijo()));
                }
                propuesta = List.copyOf(horarios);
            } catch (ProyectoNoConfirmableException ex) {
                bloqueos.addAll(ex.getBloqueos());
                if (ex.getBloqueos().isEmpty()) {
                    bloqueos.add(new BloqueoConfirmacionResponse(
                            ex.getCodigo(), ex.getMessage(), "PROYECTO", idProyecto));
                }
            } catch (RuntimeException ex) {
                bloqueos.add(new BloqueoConfirmacionResponse(
                        "CRONOGRAMA_NO_VIABLE",
                        ex.getMessage() != null
                                ? ex.getMessage()
                                : "No se pudo calcular el cronograma propuesto.",
                        "PROYECTO",
                        idProyecto));
            }
        }

        return new PrevisualizacionConfirmacionResponse(
                bloqueos.isEmpty(),
                contexto.proyecto().getEstado().getNombre(),
                ahora,
                contexto.planificacion().getFechaEntrega(),
                bloqueos,
                contexto.requerimientos().generales(),
                contexto.requerimientos().actividades(),
                propuesta);
    }
}
