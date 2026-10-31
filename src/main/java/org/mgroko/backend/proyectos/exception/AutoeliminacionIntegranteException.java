package org.mgroko.backend.proyectos.exception;

public class AutoeliminacionIntegranteException extends RuntimeException {

    public AutoeliminacionIntegranteException() {
        super("No podés eliminarte a vos mismo del proyecto. Usá la opción Darse de baja (UC-40).");
    }
}
