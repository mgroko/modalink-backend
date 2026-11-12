package org.mgroko.backend.reemplazos.servicio;

import java.util.List;

import org.mgroko.backend.reemplazos.enums.EstadoCandidatoReemplazo;
import org.mgroko.backend.reemplazos.enums.EstadoReemplazo;
import org.mgroko.backend.reemplazos.repositorio.ReemplazoCandidatoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calcula el estado agregado de un reemplazo a partir del estado de sus
 * candidatos.
 *
 * <p>Es la fuente única de verdad para no perder de vista a los candidatos que
 * todavía están {@code PROPUESTO}. Si no quedan invitaciones ni aceptaciones
 * pero sí propuestas, el proceso vuelve a quedar pendiente de que el director
 * elija a quién invitar.</p>
 */
@Service
public class EstadoReemplazoCalculator {

    private static final List<EstadoCandidatoReemplazo> ESTADOS_ACEPTADOS =
            List.of(EstadoCandidatoReemplazo.ACEPTADO_PENDIENTE_APROBACION);

    private static final List<EstadoCandidatoReemplazo> ESTADOS_INVITADOS =
            List.of(EstadoCandidatoReemplazo.INVITADO);

    private static final List<EstadoCandidatoReemplazo> ESTADOS_PROPUESTOS =
            List.of(EstadoCandidatoReemplazo.PROPUESTO);

    private final ReemplazoCandidatoRepository candidatoRepository;

    public EstadoReemplazoCalculator(ReemplazoCandidatoRepository candidatoRepository) {
        this.candidatoRepository = candidatoRepository;
    }

    @Transactional(readOnly = true)
    public EstadoReemplazo calcular(Long idReemplazo) {
        if (idReemplazo == null) {
            return EstadoReemplazo.SIN_CANDIDATOS;
        }

        if (candidatoRepository.countByReemplazoIdReemplazoAndEstadoIn(
                idReemplazo, ESTADOS_ACEPTADOS) > 0) {
            return EstadoReemplazo.PENDIENTE_APROBACION;
        }

        if (candidatoRepository.countByReemplazoIdReemplazoAndEstadoIn(
                idReemplazo, ESTADOS_INVITADOS) > 0) {
            return EstadoReemplazo.INVITACIONES_ENVIADAS;
        }

        if (candidatoRepository.countByReemplazoIdReemplazoAndEstadoIn(
                idReemplazo, ESTADOS_PROPUESTOS) > 0) {
            return EstadoReemplazo.PENDIENTE_CONFIRMACION_DIRECTOR;
        }

        return EstadoReemplazo.SIN_CANDIDATOS;
    }
}
