package org.mgroko.backend.reemplazos.servicio;

import org.mgroko.backend.proyectos.evento.CupoRequerimientoActividadModificadoEvent;
import org.mgroko.backend.proyectos.evento.CupoRequerimientoGralModificadoEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Reacciona a los cambios de cobertura de un requerimiento dentro de la misma
 * transacción.
 *
 * <p>Si la cancelación falla, se revierte también la operación que cubrió la
 * vacante para no dejar procesos de reemplazo activos sobre un requerimiento
 * que ya no los necesita.</p>
 */
@Component
public class ReemplazoCupoModificadoListener {

    private final CancelarReemplazosCumplidosService cancelarService;

    public ReemplazoCupoModificadoListener(
            CancelarReemplazosCumplidosService cancelarService) {
        this.cancelarService = cancelarService;
    }

    @EventListener
    public void alModificarCupoActividad(
            CupoRequerimientoActividadModificadoEvent evento) {
        cancelarService.cancelarPorRequerimientoActividad(evento.idRequerimiento());
    }

    @EventListener
    public void alModificarCupoGeneral(
            CupoRequerimientoGralModificadoEvent evento) {
        cancelarService.cancelarPorRequerimientoGeneral(
                evento.idRequerimientoGral());
    }
}
