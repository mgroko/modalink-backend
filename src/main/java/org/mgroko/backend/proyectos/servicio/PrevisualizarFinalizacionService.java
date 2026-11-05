package org.mgroko.backend.proyectos.servicio;

import org.mgroko.backend.proyectos.dto.MiembroCalificableResponse;
import org.mgroko.backend.proyectos.dto.PrevisualizacionFinalizacionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Previsualización de UC-37. Ejecuta las mismas validaciones que la
 * finalización, pero sin persistir ni tomar locks.
 */
@Service
public class PrevisualizarFinalizacionService {

    private final ContextoFinalizacionProyectoService contextoService;

    public PrevisualizarFinalizacionService(
            ContextoFinalizacionProyectoService contextoService) {
        this.contextoService = contextoService;
    }

    @Transactional(readOnly = true)
    public PrevisualizacionFinalizacionResponse previsualizar(
            Long idUsuario,
            Long idPerfilActivo,
            Long idProyecto) {

        ContextoFinalizacionProyectoService.Contexto contexto =
                contextoService.cargar(
                        idUsuario, idPerfilActivo, idProyecto, false);

        int requeridas = contexto.miembrosACalificar().size();
        int realizadas = (int) contexto.miembrosACalificar().stream()
                .filter(MiembroCalificableResponse::yaCalificado)
                .count();

        return new PrevisualizacionFinalizacionResponse(
                contexto.bloqueos().isEmpty(),
                contexto.proyecto().getEstado().getNombre(),
                contexto.planificacion() != null
                        ? contexto.planificacion().getFechaEntrega()
                        : null,
                contexto.ahora().toLocalDate(),
                contexto.requiereConfirmacionPrematura(),
                contexto.diasAnticipacion(),
                contexto.bloqueos(),
                contexto.advertencias(),
                contexto.miembrosACalificar(),
                requeridas,
                realizadas,
                Math.max(0, requeridas - realizadas));
    }
}
