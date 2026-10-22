package org.mgroko.backend.actividades.evento;

import java.util.List;

public record ActividadModificadaEvent(
        Long idActividad,
        String nombreActividad,
        String nombreProyecto,
        List<Long> idsPerfilesDestinatarios) {
}
