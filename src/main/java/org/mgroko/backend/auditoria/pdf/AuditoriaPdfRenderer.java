package org.mgroko.backend.auditoria.pdf;

import java.awt.Color;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.mgroko.backend.auditoria.dto.AuditoriaDetalleResponse;
import org.mgroko.backend.auditoria.dto.AuditoriaEventoResponse;
import org.mgroko.backend.auditoria.dto.AuditoriaResumenEntidadResponse;
import org.mgroko.backend.auditoria.dto.AuditoriaResumenResponse;
import org.mgroko.backend.auditoria.dto.AuditoriaResumenTipoResponse;
import org.mgroko.backend.auditoria.exception.ErrorGenerandoPdfException;
import org.openpdf.text.Chunk;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renderiza el informe PDF de auditoría (UC-67).
 *
 * <p>El diseño replica el informe administrativo: encabezado con período,
 * bloque de metadatos, indicadores, resumen por tipo y entidad, y detalle de
 * eventos agrupado. El pie de cada hoja muestra la fecha de impresión y el
 * total de hojas.</p>
 */
public class AuditoriaPdfRenderer {

    private static final Logger log =
            LoggerFactory.getLogger(AuditoriaPdfRenderer.class);

    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA =
            DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter FECHA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final float MARGEN_LATERAL = 36f;
    private static final float MARGEN_SUPERIOR = 44f;
    private static final float MARGEN_INFERIOR = 40f;

    private static final float[] ANCHOS_RESUMEN_TIPOS = {2.2f, 2.0f, 0.7f};
    private static final float[] ANCHOS_RESUMEN_ENTIDADES = {3.4f, 1.0f};
    private static final float[] ANCHOS_EVENTOS = {
            70f, 95f, 122f, 76f, 82f, 80f, 58f, 186.89f
    };

    public void render(DatosInforme datos, OutputStream salida) {
        Document document = new Document(
                PageSize.A4.rotate(),
                MARGEN_LATERAL,
                MARGEN_LATERAL,
                MARGEN_SUPERIOR,
                MARGEN_INFERIOR);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, salida);
            writer.setPageEvent(new AuditoriaPdfPageEvent(datos.impresoEn()));

            document.addTitle("Informe de auditoría · ModaLink");
            document.addAuthor("ModaLink");
            document.addSubject("Informe de auditoría");
            document.addCreator("ModaLink");

            document.open();

            document.add(encabezado(datos));
            document.add(subtitulo());
            document.add(metadatos(datos));
            document.add(indicadores(datos));
            document.add(resumenes(datos));
            document.add(titulo("Eventos", 16, 6));
            document.add(tablaEventos(datos.eventos(), datos.maxCambios()));
        } catch (DocumentException ex) {
            log.error("No se pudo generar el PDF de auditoría para el rango {} - {}",
                    datos.fechaDesde(), datos.fechaHasta(), ex);
            throw new ErrorGenerandoPdfException(
                    "No se pudo generar el PDF de auditoría.", ex);
        } finally {
            if (document.isOpen()) {
                try {
                    document.close();
                } catch (RuntimeException closeEx) {
                    log.error("Error cerrando el documento PDF de auditoría.", closeEx);
                }
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /* Encabezado                                                         */
    /* ------------------------------------------------------------------ */

    private PdfPTable encabezado(DatosInforme datos) {
        PdfPTable tabla = new PdfPTable(new float[]{1.9f, 1f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingAfter(2);

        Paragraph titulo = new Paragraph(
                "Informe de auditoría",
                AuditoriaPdfEstilos.fuente(24, Font.BOLD));
        titulo.setLeading(28);
        PdfPCell celdaTitulo = sinBorde(titulo);
        celdaTitulo.setVerticalAlignment(Element.ALIGN_TOP);

        Paragraph etiqueta = new Paragraph(
                "Período",
                AuditoriaPdfEstilos.fuente(9, Font.BOLD));
        etiqueta.setLeading(12);
        etiqueta.setAlignment(Element.ALIGN_RIGHT);

        Paragraph rango = new Paragraph(
                datos.fechaDesde().format(FECHA)
                        + " \u2013 " + datos.fechaHasta().format(FECHA),
                AuditoriaPdfEstilos.fuente(9, Font.NORMAL));
        rango.setLeading(12);
        rango.setAlignment(Element.ALIGN_RIGHT);

        PdfPCell celdaPeriodo = new PdfPCell();
        celdaPeriodo.addElement(etiqueta);
        celdaPeriodo.addElement(rango);
        celdaPeriodo.setBorder(Rectangle.NO_BORDER);
        celdaPeriodo.setPadding(0);
        celdaPeriodo.setVerticalAlignment(Element.ALIGN_TOP);

        tabla.addCell(celdaTitulo);
        tabla.addCell(celdaPeriodo);
        return tabla;
    }

    private Paragraph subtitulo() {
        Paragraph p = new Paragraph(
                "ModaLink · Registro de eventos del sistema",
                AuditoriaPdfEstilos.fuenteGris(10, Font.NORMAL));
        p.setLeading(14);
        p.setSpacingAfter(14);
        return p;
    }

    private PdfPTable metadatos(DatosInforme datos) {
        PdfPTable tabla = new PdfPTable(new float[]{2f, 1f, 1f, 1f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingAfter(12);

        tabla.addCell(meta("TIPOS DE EVENTO", datos.tiposTexto()));
        tabla.addCell(meta("GENERADO", datos.generadoEn().format(FECHA_HORA)));
        tabla.addCell(meta("GENERADO POR", datos.generadoPor()));
        tabla.addCell(meta("SISTEMA", "ModaLink"));
        return tabla;
    }

    private PdfPCell meta(String etiqueta, String valor) {
        Paragraph label = new Paragraph(
                etiqueta, AuditoriaPdfEstilos.fuenteGris(7, Font.BOLD));
        label.setLeading(9);

        Paragraph contenido = new Paragraph(
                valor == null ? "" : valor,
                AuditoriaPdfEstilos.fuente(9, Font.NORMAL));
        contenido.setLeading(11);

        PdfPCell cell = new PdfPCell();
        cell.addElement(label);
        cell.addElement(contenido);
        cell.setBackgroundColor(AuditoriaPdfEstilos.FONDO);
        cell.setBorderColor(AuditoriaPdfEstilos.BORDE);
        cell.setBorderWidth(0.6f);
        cell.setPaddingTop(7);
        cell.setPaddingBottom(7);
        cell.setPaddingLeft(10);
        cell.setPaddingRight(8);
        return cell;
    }

    /* ------------------------------------------------------------------ */
    /* Indicadores                                                        */
    /* ------------------------------------------------------------------ */

    private PdfPTable indicadores(DatosInforme datos) {
        List<AuditoriaResumenTipoResponse> tipos =
                datos.resumen().porTipoEvento();
        int columnas = 1 + Math.min(2, tipos.size());

        PdfPTable tabla = new PdfPTable(columnas);
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(2);
        tabla.setSpacingAfter(16);

        tabla.addCell(kpi(String.valueOf(datos.resumen().totalRegistros()),
                "Total de registros", true));
        for (int i = 0; i < columnas - 1; i++) {
            AuditoriaResumenTipoResponse tipo = tipos.get(i);
            tabla.addCell(kpi(String.valueOf(tipo.total()), tipo.nombre(), false));
        }
        return tabla;
    }

    private PdfPCell kpi(String valor, String etiqueta, boolean destacado) {
        Paragraph numero = new Paragraph(
                valor,
                destacado
                        ? AuditoriaPdfEstilos.fuenteAzul(22, Font.BOLD)
                        : AuditoriaPdfEstilos.fuente(22, Font.BOLD));
        numero.setLeading(24);
        numero.setSpacingAfter(2);

        Paragraph label = new Paragraph(
                etiqueta == null ? "" : etiqueta,
                AuditoriaPdfEstilos.fuenteGris(8, Font.NORMAL));
        label.setLeading(10);

        PdfPCell cell = new PdfPCell();
        cell.addElement(numero);
        cell.addElement(label);
        cell.setBackgroundColor(Color.WHITE);
        cell.setUseVariableBorders(true);
        cell.setBorder(Rectangle.LEFT | Rectangle.RIGHT
                | Rectangle.TOP | Rectangle.BOTTOM);
        cell.setBorderColorRight(AuditoriaPdfEstilos.BORDE);
        cell.setBorderColorTop(AuditoriaPdfEstilos.BORDE);
        cell.setBorderColorBottom(AuditoriaPdfEstilos.BORDE);
        cell.setBorderWidthRight(0.6f);
        cell.setBorderWidthTop(0.6f);
        cell.setBorderWidthBottom(0.6f);
        if (destacado) {
            cell.setBorderColorLeft(AuditoriaPdfEstilos.AZUL);
            cell.setBorderWidthLeft(3f);
        } else {
            cell.setBorderColorLeft(AuditoriaPdfEstilos.BORDE);
            cell.setBorderWidthLeft(0.6f);
        }
        cell.setPaddingLeft(12);
        cell.setPaddingRight(12);
        cell.setPaddingTop(10);
        cell.setPaddingBottom(8);
        return cell;
    }

    /* ------------------------------------------------------------------ */
    /* Resúmenes                                                          */
    /* ------------------------------------------------------------------ */

    private PdfPTable resumenes(DatosInforme datos) {
        PdfPTable tabla = new PdfPTable(new float[]{467.33f, 20f, 282.56f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingAfter(4);

        PdfPCell izquierda = sinBorde();
        izquierda.addElement(titulo("Resumen por tipo de evento", 0, 6));
        izquierda.addElement(tablaResumenTipos(
                datos.resumen().porTipoEvento()));

        PdfPCell derecha = sinBorde();
        derecha.addElement(titulo("Resumen por entidad afectada", 0, 6));
        derecha.addElement(tablaResumenEntidades(
                datos.resumen().porEntidad()));

        tabla.addCell(izquierda);
        tabla.addCell(sinBorde());
        tabla.addCell(derecha);
        return tabla;
    }

    private PdfPTable tablaResumenTipos(
            List<AuditoriaResumenTipoResponse> tipos) {

        PdfPTable tabla = new PdfPTable(ANCHOS_RESUMEN_TIPOS);
        tabla.setWidthPercentage(100);
        tabla.setHeaderRows(1);

        tabla.addCell(cabeceraResumen("CÓDIGO", Element.ALIGN_LEFT));
        tabla.addCell(cabeceraResumen("NOMBRE", Element.ALIGN_LEFT));
        tabla.addCell(cabeceraResumen("TOTAL", Element.ALIGN_RIGHT));

        if (tipos.isEmpty()) {
            tabla.addCell(cuerpoResumen(
                    new Phrase(9, "Sin datos",
                            AuditoriaPdfEstilos.fuenteGris(7.5f, Font.NORMAL)),
                    Element.ALIGN_LEFT, 3));
            return tabla;
        }

        for (AuditoriaResumenTipoResponse tipo : tipos) {
            tabla.addCell(cuerpoResumen(
                    new Phrase(9, texto(tipo.codigo()),
                            AuditoriaPdfEstilos.fuenteMono(7.8f)),
                    Element.ALIGN_LEFT, 1));
            tabla.addCell(cuerpoResumen(
                    new Phrase(10, texto(tipo.nombre()),
                            AuditoriaPdfEstilos.fuente(8, Font.NORMAL)),
                    Element.ALIGN_LEFT, 1));
            tabla.addCell(cuerpoResumen(
                    new Phrase(10, String.valueOf(tipo.total()),
                            AuditoriaPdfEstilos.fuente(8.5f, Font.BOLD)),
                    Element.ALIGN_RIGHT, 1));
        }
        return tabla;
    }

    private PdfPTable tablaResumenEntidades(
            List<AuditoriaResumenEntidadResponse> entidades) {

        PdfPTable tabla = new PdfPTable(ANCHOS_RESUMEN_ENTIDADES);
        tabla.setWidthPercentage(100);
        tabla.setHeaderRows(1);

        tabla.addCell(cabeceraResumen("ENTIDAD", Element.ALIGN_LEFT));
        tabla.addCell(cabeceraResumen("TOTAL", Element.ALIGN_RIGHT));

        if (entidades.isEmpty()) {
            tabla.addCell(cuerpoResumen(
                    new Phrase(9, "Sin datos",
                            AuditoriaPdfEstilos.fuenteGris(7.5f, Font.NORMAL)),
                    Element.ALIGN_LEFT, 2));
            return tabla;
        }

        for (AuditoriaResumenEntidadResponse entidad : entidades) {
            tabla.addCell(cuerpoResumen(
                    new Phrase(10, texto(entidad.entidad()),
                            AuditoriaPdfEstilos.fuente(8, Font.NORMAL)),
                    Element.ALIGN_LEFT, 1));
            tabla.addCell(cuerpoResumen(
                    new Phrase(10, String.valueOf(entidad.total()),
                            AuditoriaPdfEstilos.fuente(8.5f, Font.BOLD)),
                    Element.ALIGN_RIGHT, 1));
        }
        return tabla;
    }

    private PdfPCell cabeceraResumen(String texto, int alineacion) {
        PdfPCell cell = new PdfPCell(new Phrase(
                9, texto, AuditoriaPdfEstilos.fuenteBlanca(7.5f, Font.BOLD)));
        cell.setBackgroundColor(AuditoriaPdfEstilos.TEXTO);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(6);
        cell.setHorizontalAlignment(alineacion);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private PdfPCell cuerpoResumen(
            Phrase phrase, int alineacion, int colspan) {

        PdfPCell cell = new PdfPCell(phrase);
        cell.setColspan(colspan);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(AuditoriaPdfEstilos.BORDE);
        cell.setBorderWidth(0.5f);
        cell.setPaddingTop(5);
        cell.setPaddingBottom(5);
        cell.setPaddingLeft(6);
        cell.setPaddingRight(6);
        cell.setHorizontalAlignment(alineacion);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    /* ------------------------------------------------------------------ */
    /* Detalle de eventos                                                 */
    /* ------------------------------------------------------------------ */

    private PdfPTable tablaEventos(
            List<AuditoriaEventoResponse> eventos, int maxCambios) {

        PdfPTable tabla = new PdfPTable(ANCHOS_EVENTOS);
        tabla.setWidthPercentage(100);
        tabla.setHeaderRows(1);
        tabla.setSplitLate(false);
        tabla.setSplitRows(true);

        tabla.addCell(cabeceraEvento("Fecha", Element.ALIGN_LEFT));
        tabla.addCell(cabeceraEvento("Evento", Element.ALIGN_LEFT));
        tabla.addCell(cabeceraEvento("Entidad / ID", Element.ALIGN_LEFT));
        tabla.addCell(cabeceraEvento("Operador", Element.ALIGN_LEFT));
        tabla.addCell(cabeceraEvento("IP", Element.ALIGN_LEFT));
        tabla.addCell(cabeceraEvento("Campo", Element.ALIGN_LEFT));
        tabla.addCell(cabeceraEvento("Valor anterior", Element.ALIGN_LEFT));
        tabla.addCell(cabeceraEvento("Valor actual", Element.ALIGN_LEFT));

        if (eventos.isEmpty()) {
            PdfPCell vacio = new PdfPCell(new Phrase(
                    10, "Sin registros para los filtros seleccionados.",
                    AuditoriaPdfEstilos.fuenteGris(8, Font.NORMAL)));
            vacio.setColspan(8);
            vacio.setBackgroundColor(AuditoriaPdfEstilos.FONDO);
            vacio.setBorder(Rectangle.BOTTOM);
            vacio.setBorderColor(AuditoriaPdfEstilos.BORDE);
            vacio.setPadding(10);
            tabla.addCell(vacio);
            return tabla;
        }

        int indiceGrupo = 0;
        for (AuditoriaEventoResponse evento : eventos) {
            Color fondo = indiceGrupo % 2 == 0
                    ? AuditoriaPdfEstilos.FONDO
                    : Color.WHITE;

            List<AuditoriaDetalleResponse> detalles = evento.detalle();
            int mostrados = Math.min(detalles.size(), maxCambios);
            boolean hayMas = detalles.size() > mostrados;
            int filas = Math.max(1, mostrados) + (hayMas ? 1 : 0);

            boolean ultimaFila = filas == 1;
            tabla.addCell(celdaFecha(evento, fondo, ultimaFila));
            tabla.addCell(celdaIzquierda(
                    new Phrase(10, nombreEvento(evento),
                            AuditoriaPdfEstilos.fuente(8, Font.NORMAL)),
                    fondo, false, ultimaFila));
            tabla.addCell(celdaIzquierda(
                    new Phrase(10, entidad(evento),
                            AuditoriaPdfEstilos.fuenteMono(7.8f)),
                    fondo, false, ultimaFila));
            tabla.addCell(celdaIzquierda(
                    new Phrase(10, texto(evento.operador()),
                            AuditoriaPdfEstilos.fuente(8, Font.NORMAL)),
                    fondo, false, ultimaFila));
            tabla.addCell(celdaIzquierda(
                    new Phrase(10, texto(evento.ipUsuario()),
                            AuditoriaPdfEstilos.fuenteMono(7.8f)),
                    fondo, false, ultimaFila));

            if (detalles.isEmpty()) {
                agregarDetalle(tabla, "", "", "", fondo, ultimaFila);
            } else {
                agregarDetalle(tabla,
                        valorVisible(detalles.get(0).campo()),
                        valorVisible(detalles.get(0).valorAnterior()),
                        valorVisible(detalles.get(0).valorNuevo()),
                        fondo, ultimaFila);
            }

            for (int fila = 1; fila < filas; fila++) {
                boolean cierre = fila == filas - 1;
                tabla.addCell(celdaVacia(fondo, true, cierre));
                tabla.addCell(celdaVacia(fondo, false, cierre));
                tabla.addCell(celdaVacia(fondo, false, cierre));
                tabla.addCell(celdaVacia(fondo, false, cierre));
                tabla.addCell(celdaVacia(fondo, false, cierre));

                if (fila < mostrados) {
                    AuditoriaDetalleResponse detalle = detalles.get(fila);
                    agregarDetalle(tabla,
                            valorVisible(detalle.campo()),
                            valorVisible(detalle.valorAnterior()),
                            valorVisible(detalle.valorNuevo()),
                            fondo, cierre);
                } else {
                    agregarDetalle(tabla,
                            "+" + (detalles.size() - mostrados) + " más",
                            "", "", fondo, cierre);
                }
            }
            indiceGrupo++;
        }
        return tabla;
    }

    private PdfPCell celdaFecha(
            AuditoriaEventoResponse evento,
            Color fondo,
            boolean ultimaFila) {

        Phrase phrase = new Phrase(10);
        phrase.add(new Chunk(
                evento.fechaHora().format(FECHA),
                AuditoriaPdfEstilos.fuente(8, Font.BOLD)));
        phrase.add(Chunk.NEWLINE);
        phrase.add(new Chunk(
                evento.fechaHora().format(HORA),
                AuditoriaPdfEstilos.fuenteGris(8, Font.NORMAL)));
        return celdaIzquierda(phrase, fondo, true, ultimaFila);
    }

    private void agregarDetalle(
            PdfPTable tabla,
            String campo,
            String valorAnterior,
            String valorNuevo,
            Color fondo,
            boolean ultimaFila) {

        tabla.addCell(celdaDerecha(
                new Phrase(10, campo,
                        AuditoriaPdfEstilos.fuenteMono(7.8f)),
                fondo, ultimaFila));
        tabla.addCell(celdaDerecha(
                new Phrase(10, valorAnterior,
                        AuditoriaPdfEstilos.fuente(8, Font.NORMAL)),
                fondo, ultimaFila));
        tabla.addCell(celdaDerecha(
                new Phrase(10, valorNuevo,
                        AuditoriaPdfEstilos.fuente(8, Font.NORMAL)),
                fondo, ultimaFila));
    }

    private PdfPCell celdaIzquierda(
            Phrase phrase,
            Color fondo,
            boolean primeraColumna,
            boolean ultimaFila) {

        int borde = Rectangle.NO_BORDER;
        if (primeraColumna) {
            borde |= Rectangle.LEFT;
        }
        if (ultimaFila) {
            borde |= Rectangle.BOTTOM;
        }

        PdfPCell cell = new PdfPCell(phrase);
        cell.setBackgroundColor(fondo);
        cell.setUseVariableBorders(true);
        cell.setBorder(borde);
        cell.setBorderWidthLeft(primeraColumna ? 2.5f : 0f);
        cell.setBorderColorLeft(AuditoriaPdfEstilos.AZUL);
        cell.setBorderWidthBottom(ultimaFila ? 0.8f : 0f);
        cell.setBorderColorBottom(AuditoriaPdfEstilos.BORDE);
        cell.setPaddingTop(5);
        cell.setPaddingBottom(5);
        cell.setPaddingLeft(6);
        cell.setPaddingRight(6);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        return cell;
    }

    private PdfPCell celdaVacia(
            Color fondo, boolean primeraColumna, boolean ultimaFila) {
        return celdaIzquierda(
                new Phrase(10, " ", AuditoriaPdfEstilos.fuente(8, Font.NORMAL)),
                fondo, primeraColumna, ultimaFila);
    }

    private PdfPCell celdaDerecha(
            Phrase phrase, Color fondo, boolean ultimaFila) {

        PdfPCell cell = new PdfPCell(phrase);
        cell.setBackgroundColor(fondo);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(AuditoriaPdfEstilos.BORDE);
        cell.setBorderWidth(ultimaFila ? 0.8f : 0.4f);
        cell.setPaddingTop(5);
        cell.setPaddingBottom(5);
        cell.setPaddingLeft(6);
        cell.setPaddingRight(6);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        return cell;
    }

    private PdfPCell cabeceraEvento(String texto, int alineacion) {
        PdfPCell cell = new PdfPCell(new Phrase(
                9, texto, AuditoriaPdfEstilos.fuenteBlanca(7.5f, Font.BOLD)));
        cell.setBackgroundColor(AuditoriaPdfEstilos.TEXTO);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingTop(4);
        cell.setPaddingBottom(4);
        cell.setPaddingLeft(8);
        cell.setPaddingRight(8);
        cell.setFixedHeight(27);
        cell.setHorizontalAlignment(alineacion);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    /* ------------------------------------------------------------------ */
    /* Utilidades                                                         */
    /* ------------------------------------------------------------------ */

    private Paragraph titulo(String texto, float antes, float despues) {
        Paragraph p = new Paragraph(
                texto, AuditoriaPdfEstilos.fuente(11, Font.BOLD));
        p.setLeading(14);
        p.setSpacingBefore(antes);
        p.setSpacingAfter(despues);
        return p;
    }

    private PdfPCell sinBorde() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(0);
        return cell;
    }

    private PdfPCell sinBorde(Paragraph contenido) {
        PdfPCell cell = new PdfPCell(contenido);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(0);
        return cell;
    }

    private String nombreEvento(AuditoriaEventoResponse evento) {
        if (evento.tipoEventoNombre() != null
                && !evento.tipoEventoNombre().isBlank()) {
            return evento.tipoEventoNombre();
        }
        if (evento.tipoEvento() != null && !evento.tipoEvento().isBlank()) {
            return evento.tipoEvento();
        }
        return "Sin tipo (histórico)";
    }

    private String entidad(AuditoriaEventoResponse evento) {
        if (evento.entidadClave() != null && !evento.entidadClave().isBlank()) {
            return texto(evento.entidadAfectada())
                    + " / " + evento.entidadClave();
        }
        return texto(evento.entidadAfectada()) + " / #" + evento.idAfectado();
    }

    private String texto(String valor) {
        return valor == null ? "" : valor;
    }

    private String valorVisible(String valor) {
        return valor == null || valor.isBlank() ? "—" : valor;
    }

    /**
     * Datos ya resueltos que necesita el renderer.
     */
    public record DatosInforme(
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String tiposTexto,
            String generadoPor,
            LocalDateTime generadoEn,
            LocalDateTime impresoEn,
            AuditoriaResumenResponse resumen,
            List<AuditoriaEventoResponse> eventos,
            int maxCambios) {
    }
}
