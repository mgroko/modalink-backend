package org.mgroko.backend.proyectos.pdf;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.mgroko.backend.common.pdf.ModaLinkPdfEstilos;
import org.mgroko.backend.common.pdf.ModaLinkPdfPageEvent;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfWriter;

/**
 * Barra superior y pie del informe de proyecto.
 *
 * <p>Reutiliza la barra azul y la numeración de hojas de ModaLink, y agrega
 * el proyecto, el corte y quién generó el informe. El aviso de que el informe
 * es una foto del momento vive en el cuerpo del documento.</p>
 */
public class InformeProyectoPdfPageEvent extends ModaLinkPdfPageEvent {

    private static final DateTimeFormatter FECHA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int MAX_PROYECTO_ENCABEZADO = 55;

    private final String proyecto;
    private final String corte;

    public InformeProyectoPdfPageEvent(
            LocalDateTime impresoEn,
            LocalDateTime generadoEn,
            String proyecto,
            String generadoPor) {
        super(
                impresoEn,
                "ModaLink · Informe de proyecto",
                "Impreso el " + impresoEn.format(FECHA_HORA)
                        + " · Generado por " + texto(generadoPor));
        this.proyecto = truncar(proyecto, MAX_PROYECTO_ENCABEZADO);
        this.corte = generadoEn != null ? generadoEn.format(FECHA_HORA) : "";
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        super.onEndPage(writer, document);

        PdfContentByte canvas = writer.getDirectContent();
        float y = document.getPageSize().getHeight() - 18;

        ColumnText.showTextAligned(
                canvas,
                Element.ALIGN_LEFT,
                new Phrase("ModaLink · Informe de proyecto",
                        ModaLinkPdfEstilos.fuenteGris(7.5f, Font.NORMAL)),
                document.left(),
                y,
                0);

        String derecha = proyecto
                + (corte.isBlank() ? "" : " · Corte " + corte);

        ColumnText.showTextAligned(
                canvas,
                Element.ALIGN_RIGHT,
                new Phrase(derecha, ModaLinkPdfEstilos.fuenteGris(7.5f, Font.NORMAL)),
                document.right(),
                y,
                0);
    }

    private static String truncar(String valor, int maximo) {
        if (valor == null || valor.isBlank()) {
            return "Proyecto";
        }
        String limpio = valor.trim();
        return limpio.length() <= maximo
                ? limpio
                : limpio.substring(0, maximo - 1) + "…";
    }

    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? "—" : valor;
    }
}
