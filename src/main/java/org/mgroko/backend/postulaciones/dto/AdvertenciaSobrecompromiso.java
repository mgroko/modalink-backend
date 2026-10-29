package org.mgroko.backend.postulaciones.dto;

import java.time.LocalDateTime;

/**
 * Advertencia no bloqueante por posible sobrecompromiso del profesional.
 *
 * <p>Se calcula al aceptar una postulación cuando el mismo usuario tiene otras
 * actividades aceptadas sin horario persistido y sus ventanas CPM se
 * superponen. No impide la aceptación.</p>
 */
public record AdvertenciaSobrecompromiso(
        Long idActividad,
        String nombreActividad,
        Long idProyecto,
        String nombreProyecto,
        LocalDateTime inicioPotencial,
        LocalDateTime finPotencial) {
}
