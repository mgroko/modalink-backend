package org.mgroko.backend.common.pdf;

import java.awt.Color;
import java.io.IOException;

import org.openpdf.text.DocumentException;
import org.openpdf.text.Font;
import org.openpdf.text.pdf.BaseFont;

/**
 * Paleta, tipografías y tamaños compartidos por los PDF de ModaLink.
 *
 * <p>La usan tanto el informe de auditoría (UC-67) como la exportación de
 * contratos (UC-68). Mantener una única fuente de estilos asegura que el
 * encabezado y el pie de ambos documentos tengan la misma identidad visual.</p>
 */
public final class ModaLinkPdfEstilos {

    /** Azul de marca usado en la barra superior, indicadores y acentos. */
    public static final Color AZUL = new Color(0x2F, 0x5B, 0xEA);
    /** Texto principal. */
    public static final Color TEXTO = new Color(0x1B, 0x23, 0x33);
    /** Texto secundario y etiquetas. */
    public static final Color GRIS = new Color(0x5B, 0x65, 0x77);
    /** Bordes y separadores. */
    public static final Color BORDE = new Color(0xD9, 0xDE, 0xE7);
    /** Fondo de tarjetas y filas alternadas. */
    public static final Color FONDO = new Color(0xF3, 0xF5, 0xF9);

    private static final BaseFont HELVETICA = crear(BaseFont.HELVETICA);
    private static final BaseFont HELVETICA_BOLD = crear(BaseFont.HELVETICA_BOLD);
    private static final BaseFont COURIER = crear(BaseFont.COURIER);

    private ModaLinkPdfEstilos() {
    }

    public static Font fuente(float tamanio, int estilo) {
        return conColor(HELVETICA, tamanio, estilo, TEXTO);
    }

    public static Font fuenteAzul(float tamanio, int estilo) {
        return conColor(HELVETICA, tamanio, estilo, AZUL);
    }

    public static Font fuenteGris(float tamanio, int estilo) {
        return conColor(HELVETICA, tamanio, estilo, GRIS);
    }

    public static Font fuenteBlanca(float tamanio, int estilo) {
        return conColor(HELVETICA, tamanio, estilo, Color.WHITE);
    }

    public static Font fuenteMono(float tamanio) {
        return conColor(COURIER, tamanio, Font.NORMAL, TEXTO);
    }

    public static BaseFont helveticaNegrita() {
        return HELVETICA_BOLD;
    }

    private static Font conColor(
            BaseFont base, float tamanio, int estilo, Color color) {
        Font font = new Font(
                esNegrita(estilo) ? HELVETICA_BOLD : base,
                tamanio,
                Font.NORMAL);
        font.setColor(color);
        return font;
    }

    private static boolean esNegrita(int estilo) {
        return (estilo & Font.BOLD) != 0;
    }

    private static BaseFont crear(String nombre) {
        try {
            return BaseFont.createFont(
                    nombre, BaseFont.WINANSI, BaseFont.EMBEDDED);
        } catch (DocumentException | IOException ex) {
            throw new IllegalStateException(
                    "No se pudo cargar la fuente del PDF.", ex);
        }
    }
}
