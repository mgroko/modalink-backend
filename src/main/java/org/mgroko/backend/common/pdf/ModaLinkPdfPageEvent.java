package org.mgroko.backend.common.pdf;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.openpdf.text.BadElementException;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Image;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfTemplate;
import org.openpdf.text.pdf.PdfWriter;

/**
 * Barra superior de marca y pie de página comunes a los PDF de ModaLink.
 *
 * <p>El pie se repite en todas las hojas e informa la fecha de impresión y el
 * número de hoja con el total. El total se escribe al cerrar el documento,
 * usando una {@link PdfTemplate} como marcador de posición, de modo que el
 * documento se genera en un solo pase.</p>
 *
 * <p>El texto central del pie se recibe por constructor para que cada tipo de
 * documento conserve su identidad (“Informe de auditoría”, “Contrato”, etc.)
 * sin duplicar la lógica de dibujo.</p>
 */
public class ModaLinkPdfPageEvent extends PdfPageEventHelper {

    private static final DateTimeFormatter FECHA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final float ALTO_BARRA = 8f;
    private static final float TAMANIO_PIE = 7.5f;
    private static final float ANCHO_TOTAL_HOJAS = 18f;

    private final LocalDateTime impresoEn;
    private final String textoPie;
    private final String textoIzquierda;

    private PdfTemplate totalHojas;
    private int ultimaHoja = 1;

    public ModaLinkPdfPageEvent(LocalDateTime impresoEn, String textoPie) {
        this(impresoEn, textoPie, null);
    }

    public ModaLinkPdfPageEvent(
            LocalDateTime impresoEn,
            String textoPie,
            String textoIzquierda) {
        this.impresoEn = impresoEn;
        this.textoPie = textoPie == null || textoPie.isBlank()
                ? "ModaLink"
                : textoPie;
        this.textoIzquierda = textoIzquierda;
    }

    @Override
    public void onOpenDocument(PdfWriter writer, Document document) {
        totalHojas = writer.getDirectContent().createTemplate(
                ANCHO_TOTAL_HOJAS, 10);
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        ultimaHoja = writer.getPageNumber();

        PdfContentByte canvas = writer.getDirectContent();
        dibujarBarraSuperior(canvas, document);
        dibujarPie(canvas, document, writer.getPageNumber());
    }

    @Override
    public void onCloseDocument(PdfWriter writer, Document document) {
        if (totalHojas == null) {
            return;
        }
        totalHojas.beginText();
        totalHojas.setFontAndSize(
                ModaLinkPdfEstilos.helveticaNegrita(), TAMANIO_PIE);
        totalHojas.setColorFill(ModaLinkPdfEstilos.TEXTO);
        totalHojas.setTextMatrix(0, 0);
        totalHojas.showText(String.valueOf(ultimaHoja));
        totalHojas.endText();
    }

    private void dibujarBarraSuperior(
            PdfContentByte canvas, Document document) {
        float ancho = document.getPageSize().getWidth();
        float alto = document.getPageSize().getHeight();

        canvas.saveState();
        canvas.setColorFill(ModaLinkPdfEstilos.AZUL);
        canvas.rectangle(0, alto - ALTO_BARRA, ancho, ALTO_BARRA);
        canvas.fill();
        canvas.restoreState();
    }

    private void dibujarPie(
            PdfContentByte canvas, Document document, int hoja) {

        float izquierda = document.left();
        float derecha = document.right();
        float yLinea = document.bottom();
        float yTexto = document.bottom() - 12;

        canvas.saveState();
        canvas.setColorStroke(ModaLinkPdfEstilos.BORDE);
        canvas.setLineWidth(0.6f);
        canvas.moveTo(izquierda, yLinea);
        canvas.lineTo(derecha, yLinea);
        canvas.stroke();
        canvas.restoreState();

        ColumnText.showTextAligned(
                canvas,
                Element.ALIGN_LEFT,
                new Phrase(textoIzquierda != null && !textoIzquierda.isBlank()
                                ? textoIzquierda
                                : "Impreso el " + impresoEn.format(FECHA_HORA),
                        ModaLinkPdfEstilos.fuenteGris(
                                TAMANIO_PIE, Font.NORMAL)),
                izquierda,
                yTexto,
                0);

        ColumnText.showTextAligned(
                canvas,
                Element.ALIGN_CENTER,
                new Phrase(textoPie,
                        ModaLinkPdfEstilos.fuenteGris(
                                TAMANIO_PIE, Font.NORMAL)),
                (izquierda + derecha) / 2,
                yTexto,
                0);

        dibujarNumeroHoja(canvas, derecha, yTexto, hoja);
    }

    /**
     * El texto se dibuja por separado del total: {@code ColumnText} no
     * compone frases que mezclan texto e imágenes. El total se reserva a la
     * derecha y se completa en {@link #onCloseDocument}.
     */
    private void dibujarNumeroHoja(
            PdfContentByte canvas, float derecha, float yTexto, int hoja) {

        String etiqueta = "Hoja " + hoja + " / ";
        float anchoEtiqueta = ModaLinkPdfEstilos.helveticaNegrita()
                .getWidthPoint(etiqueta, TAMANIO_PIE);

        ColumnText.showTextAligned(
                canvas,
                Element.ALIGN_LEFT,
                new Phrase(TAMANIO_PIE, etiqueta,
                        ModaLinkPdfEstilos.fuente(TAMANIO_PIE, Font.BOLD)),
                derecha - ANCHO_TOTAL_HOJAS - anchoEtiqueta,
                yTexto,
                0);

        if (totalHojas == null) {
            return;
        }
        try {
            Image total = Image.getInstance(totalHojas);
            total.setAbsolutePosition(
                    derecha - ANCHO_TOTAL_HOJAS, yTexto);
            canvas.addImage(total);
        } catch (BadElementException ex) {
            // El pie es informativo: no debe interrumpir la exportación.
        }
    }
}
