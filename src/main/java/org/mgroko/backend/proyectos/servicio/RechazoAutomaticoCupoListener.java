package org.mgroko.backend.proyectos.servicio;

import org.mgroko.backend.proyectos.evento.CupoRequerimientoActividadModificadoEvent;
import org.mgroko.backend.proyectos.evento.CupoRequerimientoGralModificadoEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Dispara el rechazo automático de postulaciones e invitaciones pendientes
 * cada vez que cambia la cobertura de un requerimiento.
 *
 * <p>Los eventos se publican dentro de la transacción que cubre la vacante,
 * por lo que el rechazo y sus notificaciones se confirman o revierten junto
 * con la operación original.</p>
 */
@Component
public class RechazoAutomaticoCupoListener {

    private final RechazoAutomaticoCupoService rechazoAutomaticoCupoService;

    public RechazoAutomaticoCupoListener(
            RechazoAutomaticoCupoService rechazoAutomaticoCupoService) {
        this.rechazoAutomaticoCupoService = rechazoAutomaticoCupoService;
    }

    @EventListener
    public void alModificarCupoActividad(
            CupoRequerimientoActividadModificadoEvent evento) {
        rechazoAutomaticoCupoService
                .rechazarPendientesSiCupoCompletoActividad(evento.idRequerimiento());
    }

    @EventListener
    public void alModificarCupoGeneral(
            CupoRequerimientoGralModificadoEvent evento) {
        rechazoAutomaticoCupoService
                .rechazarPendientesSiCupoCompletoGeneral(evento.idRequerimientoGral());
    }
}
