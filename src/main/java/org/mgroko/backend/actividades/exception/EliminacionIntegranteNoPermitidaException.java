package org.mgroko.backend.actividades.exception;

public class EliminacionIntegranteNoPermitidaException extends RuntimeException {

    public EliminacionIntegranteNoPermitidaException(String motivo) {
        super(mensaje(motivo));
    }

    private static String mensaje(String motivo) {
        if (motivo == null) {
            return "No se puede eliminar al integrante del proyecto.";
        }

        return switch (motivo) {
            case "AUTOELIMINACION" ->
                    "No podés eliminarte a vos mismo del proyecto. Usá la opción Darse de baja (UC-40).";
            case "ULTIMO_DIRECTOR" ->
                    "No se puede eliminar al último director activo del proyecto.";
            case "PROYECTO_BORRADOR" ->
                    "No se pueden eliminar integrantes de un proyecto en borrador.";
            default -> "No se puede eliminar al integrante del proyecto.";
        };
    }
}
