package org.mgroko.backend.auditoria.enums;

import java.util.Locale;

import org.mgroko.backend.auditoria.exception.FormatoExportacionInvalidoException;

public enum FormatoExportacion {
    CSV,
    PDF;

    public static FormatoExportacion desde(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new FormatoExportacionInvalidoException(
                    "El formato de exportación es obligatorio. Valores permitidos: CSV, PDF.");
        }
        try {
            return valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new FormatoExportacionInvalidoException(
                    "Formato de exportación inválido: " + valor + ". Valores permitidos: CSV, PDF.");
        }
    }
}
