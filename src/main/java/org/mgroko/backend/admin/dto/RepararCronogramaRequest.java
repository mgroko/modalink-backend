package org.mgroko.backend.admin.dto;

/**
 * Parámetros de la reparación controlada de cronogramas publicados o
 * confirmados.
 *
 * @param dryRun     {@code true} para previsualizar sin persistir
 * @param idProyecto proyecto puntual; {@code null} para todos
 * @param idActividad actividad puntual; {@code null} para todas
 * @param limite     máximo de proyectos a procesar; {@code null} sin límite
 */
public record RepararCronogramaRequest(
        Boolean dryRun,
        Long idProyecto,
        Long idActividad,
        Integer limite) {

    public boolean esDryRun() {
        return dryRun == null || dryRun;
    }
}
