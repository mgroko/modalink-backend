package org.mgroko.backend.actividades.evento;

import java.util.List;

public record ActividadModificadaEvent(
        Long idActividad,
        Long idProyecto,
        String nombreActividad,
        String nombreProyecto,
        List<Long> idsPerfilesDestinatarios) {
}
