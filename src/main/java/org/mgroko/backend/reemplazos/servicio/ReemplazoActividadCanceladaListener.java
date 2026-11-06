package org.mgroko.backend.reemplazos.servicio;

import org.mgroko.backend.actividades.evento.ActividadCanceladaEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Reacciona a la cancelación de una actividad dentro de la misma transacción.
 *
 * <p>Si la cancelación de los reemplazos falla, se revierte también la
 * cancelación de la actividad para no dejar invitaciones activas sobre una
 * actividad cancelada.</p>
 */
@Component
public class ReemplazoActividadCanceladaListener {

    private final CancelarReemplazosPorActividadService cancelarService;

    public ReemplazoActividadCanceladaListener(
            CancelarReemplazosPorActividadService cancelarService) {
        this.cancelarService = cancelarService;
    }

    @EventListener
    public void alCancelarActividad(ActividadCanceladaEvent evento) {
        cancelarService.cancelarPorActividad(
                evento.idActividad(),
                evento.nombreActividad(),
                evento.nombreProyecto());
    }
}
