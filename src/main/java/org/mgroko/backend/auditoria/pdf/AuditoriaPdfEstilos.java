package org.mgroko.backend.auditoria.pdf;

import java.awt.Color;

import org.mgroko.backend.common.pdf.ModaLinkPdfEstilos;
import org.openpdf.text.Font;
import org.openpdf.text.pdf.BaseFont;

/**
 * Paleta, tipografías y tamaños del informe PDF de auditoría (UC-67).
 *
 * <p>Delega en {@link ModaLinkPdfEstilos} para compartir la identidad visual
 * con el resto de los PDF de la aplicación sin romper la API histórica del
 * módulo de auditoría.</p>
 */
public final class AuditoriaPdfEstilos {

    /** Azul de marca usado en indicadores, títulos y acentos. */
    public static final Color AZUL = ModaLinkPdfEstilos.AZUL;
    /** Texto principal. */
    public static final Color TEXTO = ModaLinkPdfEstilos.TEXTO;
    /** Texto secundario y etiquetas. */
    public static final Color GRIS = ModaLinkPdfEstilos.GRIS;
    /** Bordes y separadores. */
    public static final Color BORDE = ModaLinkPdfEstilos.BORDE;
    /** Fondo de tarjetas y filas alternadas. */
    public static final Color FONDO = ModaLinkPdfEstilos.FONDO;

    private AuditoriaPdfEstilos() {
    }

    public static Font fuente(float tamanio, int estilo) {
        return ModaLinkPdfEstilos.fuente(tamanio, estilo);
    }

    public static Font fuenteAzul(float tamanio, int estilo) {
        return ModaLinkPdfEstilos.fuenteAzul(tamanio, estilo);
    }

    public static Font fuenteGris(float tamanio, int estilo) {
        return ModaLinkPdfEstilos.fuenteGris(tamanio, estilo);
    }

    public static Font fuenteBlanca(float tamanio, int estilo) {
        return ModaLinkPdfEstilos.fuenteBlanca(tamanio, estilo);
    }

    public static Font fuenteMono(float tamanio) {
        return ModaLinkPdfEstilos.fuenteMono(tamanio);
    }

    static BaseFont helveticaNegrita() {
        return ModaLinkPdfEstilos.helveticaNegrita();
    }
}
