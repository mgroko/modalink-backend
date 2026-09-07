package org.mgroko.backend.perfiles.exception;

public class PerfilActivoNoSeleccionadoException extends RuntimeException {
    public PerfilActivoNoSeleccionadoException() {
        super("No hay un perfil activo seleccionado en la sesión.");
    }
}
