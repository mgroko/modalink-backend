package org.mgroko.backend.proyectos.exception;

import java.util.List;

public class InformacionBasicaIncompletaException extends RuntimeException {

    public InformacionBasicaIncompletaException(List<String> camposFaltantes) {
        super("El proyecto tiene información básica incompleta. Faltan: "
                + String.join(", ", camposFaltantes) + ".");
    }
}
