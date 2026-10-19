package org.mgroko.backend.proyectos.servicio;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.modelo.Ubicacion;
import org.mgroko.backend.proyectos.exception.InformacionBasicaIncompletaException;

/**
 * Valida que un proyecto cuente con la información básica exigida para estar
 * publicado o confirmado (ubicación, fecha de entrega, moodboard y al menos un
 * requerimiento de personal). La usan tanto UC-25 Publicar como UC-36 Modificar.
 */
public final class ProyectoInformacionBasicaValidator {

    private ProyectoInformacionBasicaValidator() {
    }

    public static void validar(
            Ubicacion ubicacion,
            LocalDate fechaEntrega,
            boolean tieneMoodboard,
            boolean tieneRequerimiento) {

        List<String> faltantes = new ArrayList<>();

        if (ubicacion == null) {
            faltantes.add("ubicación");
        }
        if (fechaEntrega == null) {
            faltantes.add("fecha de entrega");
        }
        if (!tieneMoodboard) {
            faltantes.add("moodboard");
        }
        if (!tieneRequerimiento) {
            faltantes.add("al menos un requerimiento de personal");
        }

        if (!faltantes.isEmpty()) {
            throw new InformacionBasicaIncompletaException(faltantes);
        }
    }
}
