package org.mgroko.backend.repositorio.proyeccion;

import java.time.LocalDate;

/**
 * Fecha de entrega de la planificación de un proyecto.
 */
public interface PlanificacionEntregaProyeccion {

    Long getIdProyecto();

    LocalDate getFechaEntrega();
}
