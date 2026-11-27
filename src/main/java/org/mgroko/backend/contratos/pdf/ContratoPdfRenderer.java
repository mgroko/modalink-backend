package org.mgroko.backend.contratos.pdf;

import java.awt.Color;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.mgroko.backend.common.pdf.ModaLinkPdfEstilos;
import org.mgroko.backend.common.pdf.ModaLinkPdfPageEvent;
import org.mgroko.backend.contratos.exception.ErrorGenerandoPdfContratoException;
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
 * Renderiza el PDF de un contrato (UC-68).
 *
 * <p>Reutiliza la identidad visual del informe de auditoría: barra superior,
 * encabezado con título y bloque derecho, franja de metadatos, indicadores y
 * pie con fecha de impresión y “Hoja X / Y”. El contenido propio del contrato
 * se organiza en datos generales, partes y firmas, y cláusulas.</p>
 *
 * <p>El texto legal se imprime tal como quedó guardado en la versión
 * exportada. No se parsea ni se renumera para no alterar el contenido
 * contractual.</p>
 */
public class ContratoPdfRenderer {

    private static final Logger log =
            LoggerFactory.getLogger(ContratoPdfRenderer.class);

    private static final DateTimeFormatter FECHA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FECHA_HORA_SEGUNDOS =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final float MARGEN_LATERAL = 36f;
    private static final float MARGEN_SUPERIOR = 44f;
    private static final float MARGEN_INFERIOR = 40f;

    private static final Pattern ITEM = Pattern.compile(
            "^\\s*(\\d+[.)]|[-•*])\\s+.*");

    private static final Color ROJO = new Color(0xB4, 0x23, 0x18);
    private static final Color AMBAR = new Color(0xB2, 0x6A, 0x00);
    private static final Color VERDE = new Color(0x1E, 0x7B, 0x45);

    public void render(DatosContratoPdf datos, OutputStream salida) {
        Document document = new Document(
                PageSize.A4,
                MARGEN_LATERAL,
                MARGEN_LATERAL,
                MARGEN_SUPERIOR,
                MARGEN_INFERIOR);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, salida);
            writer.setPageEvent(new ModaLinkPdfPageEvent(
                    datos.impresoEn(), "ModaLink · Contrato"));

            document.addTitle("Contrato · " + texto(datos.titulo()));
            document.addAuthor("ModaLink");
            document.addSubject("Contrato " + texto(datos.tipoAcuerdo())
                    + " · " + texto(datos.proyecto()));
            document.addCreator("ModaLink");

            document.open();

            document.add(encabezado(datos));
            document.add(subtitulo(datos));
            document.add(metadatos(datos));
            document.add(indicadores(datos));
            document.add(titulo("Datos del contrato", 0, 6));
            document.add(tablaDatos(datos));
            document.add(titulo("Partes y firmas", 0, 6));
            document.add(tablaPartes(datos));
            agregarMotivosRechazo(document, datos);
            document.add(titulo("Cláusulas", 12, 6));
            agregarClausulas(document, datos.texto());
        } catch (DocumentException ex) {
            log.error("No se pudo generar el PDF del contrato {}",
                    datos.idContrato(), ex);
            throw new ErrorGenerandoPdfContratoException(
                    "No se pudo generar el PDF del contrato.", ex);
        } finally {
            if (document.isOpen()) {
                try {
                    document.close();
                } catch (RuntimeException closeEx) {
                    log.error("Error cerrando el documento PDF del contrato.",
                            closeEx);
                }
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /* Encabezado                                                         */
    /* ------------------------------------------------------------------ */

    private PdfPTable encabezado(DatosContratoPdf datos) {
        PdfPTable tabla = new PdfPTable(new float[]{1.9f, 1f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingAfter(2);

        Paragraph antetitulo = new Paragraph(
                "CONTRATO",
                ModaLinkPdfEstilos.fuenteAzul(8, Font.BOLD));
        antetitulo.setLeading(10);
        antetitulo.setSpacingAfter(2);

        Paragraph titulo = new Paragraph(
                valor(datos.titulo()),
                ModaLinkPdfEstilos.fuente(18, Font.BOLD));
        titulo.setLeading(21);

        PdfPCell celdaTitulo = new PdfPCell();
        celdaTitulo.addElement(antetitulo);
        celdaTitulo.addElement(titulo);
        celdaTitulo.setBorder(Rectangle.NO_BORDER);
        celdaTitulo.setPadding(0);
        celdaTitulo.setVerticalAlignment(Element.ALIGN_TOP);

        Paragraph etiquetaEmision = new Paragraph(
                "EMISIÓN",
                ModaLinkPdfEstilos.fuenteGris(8, Font.BOLD));
        etiquetaEmision.setLeading(10);
        etiquetaEmision.setAlignment(Element.ALIGN_RIGHT);

        Paragraph valorEmision = new Paragraph(
                fecha(datos.fechaEmision(), FECHA_HORA, "Sin emitir"),
                ModaLinkPdfEstilos.fuente(8.5f, Font.NORMAL));
        valorEmision.setLeading(11);
        valorEmision.setAlignment(Element.ALIGN_RIGHT);

        Paragraph etiquetaVersion = new Paragraph(
                "VERSIÓN",
                ModaLinkPdfEstilos.fuenteGris(8, Font.BOLD));
        etiquetaVersion.setLeading(10);
        etiquetaVersion.setAlignment(Element.ALIGN_RIGHT);

        Paragraph valorVersion = new Paragraph(
                "v" + version(datos.version()),
                ModaLinkPdfEstilos.fuente(8.5f, Font.NORMAL));
        valorVersion.setLeading(11);
        valorVersion.setAlignment(Element.ALIGN_RIGHT);

        Font fuenteEstado = ModaLinkPdfEstilos.fuente(8.5f, Font.BOLD);
        fuenteEstado.setColor(colorEstado(datos.estado()));

        Paragraph estado = new Paragraph(
                estadoContrato(datos.estado()),
                fuenteEstado);
        estado.setLeading(11);
        estado.setAlignment(Element.ALIGN_RIGHT);

        PdfPCell celdaDerecha = new PdfPCell();
        celdaDerecha.addElement(etiquetaEmision);
        celdaDerecha.addElement(valorEmision);
        celdaDerecha.addElement(etiquetaVersion);
        celdaDerecha.addElement(valorVersion);
        celdaDerecha.addElement(estado);
        celdaDerecha.setBorder(Rectangle.NO_BORDER);
        celdaDerecha.setPadding(0);
        celdaDerecha.setVerticalAlignment(Element.ALIGN_TOP);

        tabla.addCell(celdaTitulo);
        tabla.addCell(celdaDerecha);
        return tabla;
    }

    private Paragraph subtitulo(DatosContratoPdf datos) {
        Paragraph p = new Paragraph(
                "ModaLink · " + valor(datos.proyecto())
                        + " · " + valor(datos.tipoAcuerdo()),
                ModaLinkPdfEstilos.fuenteGris(10, Font.NORMAL));
        p.setLeading(14);
        p.setSpacingAfter(14);
        return p;
    }

    private PdfPTable metadatos(DatosContratoPdf datos) {
        PdfPTable tabla = new PdfPTable(new float[]{1.6f, 1.15f, 1.35f, 0.9f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingAfter(12);

        tabla.addCell(meta("TIPO DE ACUERDO", datos.tipoAcuerdo()));
        tabla.addCell(meta("GENERADO",
                datos.exportadoEn() == null
                        ? ""
                        : datos.exportadoEn().format(FECHA_HORA_SEGUNDOS)));
        tabla.addCell(meta("GENERADO POR", datos.exportadoPor()));
        tabla.addCell(meta("SISTEMA", "ModaLink"));
        return tabla;
    }

    private PdfPCell meta(String etiqueta, String valor) {
        Paragraph label = new Paragraph(
                etiqueta, ModaLinkPdfEstilos.fuenteGris(7, Font.BOLD));
        label.setLeading(9);

        Paragraph contenido = new Paragraph(
                valor(valor),
                ModaLinkPdfEstilos.fuente(9, Font.NORMAL));
        contenido.setLeading(11);

        PdfPCell cell = new PdfPCell();
        cell.addElement(label);
        cell.addElement(contenido);
        cell.setBackgroundColor(ModaLinkPdfEstilos.FONDO);
        cell.setBorderColor(ModaLinkPdfEstilos.BORDE);
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

    private PdfPTable indicadores(DatosContratoPdf datos) {
        PdfPTable tabla = new PdfPTable(3);
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(2);
        tabla.setSpacingAfter(16);

        tabla.addCell(kpi(
                datos.firmados() + " / " + datos.totalPartes(),
                "Firmas registradas",
                true));
        tabla.addCell(kpi(
                String.valueOf(version(datos.version())),
                "Versión del texto",
                false));
        tabla.addCell(kpi(
                String.valueOf(datos.partes() == null ? 0 : datos.partes().size()),
                "Partes",
                false));
        return tabla;
    }

    private PdfPCell kpi(String valor, String etiqueta, boolean destacado) {
        Paragraph numero = new Paragraph(
                valor,
                destacado
                        ? ModaLinkPdfEstilos.fuenteAzul(22, Font.BOLD)
                        : ModaLinkPdfEstilos.fuente(22, Font.BOLD));
        numero.setLeading(24);
        numero.setSpacingAfter(2);

        Paragraph label = new Paragraph(
                etiqueta == null ? "" : etiqueta,
                ModaLinkPdfEstilos.fuenteGris(8, Font.NORMAL));
        label.setLeading(10);

        PdfPCell cell = new PdfPCell();
        cell.addElement(numero);
        cell.addElement(label);
        cell.setBackgroundColor(Color.WHITE);
        cell.setUseVariableBorders(true);
        cell.setBorder(Rectangle.LEFT | Rectangle.RIGHT
                | Rectangle.TOP | Rectangle.BOTTOM);
        cell.setBorderColorRight(ModaLinkPdfEstilos.BORDE);
        cell.setBorderColorTop(ModaLinkPdfEstilos.BORDE);
        cell.setBorderColorBottom(ModaLinkPdfEstilos.BORDE);
        cell.setBorderWidthRight(0.6f);
        cell.setBorderWidthTop(0.6f);
        cell.setBorderWidthBottom(0.6f);
        if (destacado) {
            cell.setBorderColorLeft(ModaLinkPdfEstilos.AZUL);
            cell.setBorderWidthLeft(3f);
        } else {
            cell.setBorderColorLeft(ModaLinkPdfEstilos.BORDE);
            cell.setBorderWidthLeft(0.6f);
        }
        cell.setPaddingLeft(12);
        cell.setPaddingRight(12);
        cell.setPaddingTop(10);
        cell.setPaddingBottom(8);
        return cell;
    }

    /* ------------------------------------------------------------------ */
    /* Datos generales y partes                                           */
    /* ------------------------------------------------------------------ */

    private PdfPTable tablaDatos(DatosContratoPdf datos) {
        PdfPTable tabla = new PdfPTable(new float[]{1f, 1.65f, 1f, 1.65f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingAfter(12);

        filaDato(tabla,
                "Proyecto", datos.proyecto(),
                "Tipo de acuerdo", datos.tipoAcuerdo());
        filaDato(tabla,
                "Emisor", datos.emisorNombre(),
                "Estado", estadoContrato(datos.estado()));
        filaDato(tabla,
                "Versión", "v" + version(datos.version()),
                "Creación", fecha(datos.fechaCreacion(), FECHA_HORA, "—"));
        filaDato(tabla,
                "Emisión", fecha(datos.fechaEmision(), FECHA_HORA, "—"),
                "Modificación", fecha(datos.fechaModificacion(), FECHA_HORA, "—"));
        filaDato(tabla,
                "Baja", fecha(datos.fechaBaja(), FECHA_HORA, "—"),
                "Cancelación", fecha(datos.fechaCancelacion(), FECHA_HORA, "—"));

        if (datos.motivoCancelacion() != null
                && !datos.motivoCancelacion().isBlank()) {
            filaDato(tabla,
                    "Motivo de cancelación", datos.motivoCancelacion(),
                    "—", null);
        }
        return tabla;
    }

    private void filaDato(
            PdfPTable tabla,
            String etiqueta1,
            String valor1,
            String etiqueta2,
            String valor2) {

        tabla.addCell(celdaEtiquetaDato(etiqueta1));
        tabla.addCell(celdaValorDato(valor1));
        tabla.addCell(celdaEtiquetaDato(etiqueta2));
        tabla.addCell(celdaValorDato(valor2));
    }

    private PdfPCell celdaEtiquetaDato(String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(
                9, texto,
                ModaLinkPdfEstilos.fuenteGris(7.5f, Font.BOLD)));
        cell.setBackgroundColor(ModaLinkPdfEstilos.FONDO);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(ModaLinkPdfEstilos.BORDE);
        cell.setBorderWidth(0.6f);
        cell.setPaddingTop(6);
        cell.setPaddingBottom(6);
        cell.setPaddingLeft(8);
        cell.setPaddingRight(6);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private PdfPCell celdaValorDato(String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(
                9, valor(texto),
                ModaLinkPdfEstilos.fuente(8.5f, Font.NORMAL)));
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(ModaLinkPdfEstilos.BORDE);
        cell.setBorderWidth(0.6f);
        cell.setPaddingTop(6);
        cell.setPaddingBottom(6);
        cell.setPaddingLeft(8);
        cell.setPaddingRight(6);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private PdfPTable tablaPartes(DatosContratoPdf datos) {
        PdfPTable tabla = new PdfPTable(new float[]{2.5f, 0.9f, 1.2f, 1.05f, 1.25f});
        tabla.setWidthPercentage(100);
        tabla.setHeaderRows(1);
        tabla.setSpacingAfter(4);

        tabla.addCell(cabecera("PARTE", Element.ALIGN_LEFT));
        tabla.addCell(cabecera("DNI", Element.ALIGN_LEFT));
        tabla.addCell(cabecera("ROL", Element.ALIGN_LEFT));
        tabla.addCell(cabecera("ESTADO", Element.ALIGN_LEFT));
        tabla.addCell(cabecera("FIRMA / RECHAZO", Element.ALIGN_LEFT));

        List<PartePdf> partes = datos.partes();
        if (partes == null || partes.isEmpty()) {
            PdfPCell vacio = new PdfPCell(new Phrase(
                    10, "Sin partes registradas.",
                    ModaLinkPdfEstilos.fuenteGris(8, Font.NORMAL)));
            vacio.setColspan(5);
            vacio.setBackgroundColor(ModaLinkPdfEstilos.FONDO);
            vacio.setBorder(Rectangle.BOTTOM);
            vacio.setBorderColor(ModaLinkPdfEstilos.BORDE);
            vacio.setPadding(10);
            tabla.addCell(vacio);
            return tabla;
        }

        int indice = 0;
        for (PartePdf parte : partes) {
            Color fondo = indice % 2 == 0
                    ? ModaLinkPdfEstilos.FONDO
                    : Color.WHITE;

            tabla.addCell(celdaParte(valor(parte.nombre()),
                    ModaLinkPdfEstilos.fuente(8, Font.BOLD), fondo));
            tabla.addCell(celdaParte(valor(parte.dni()),
                    ModaLinkPdfEstilos.fuenteMono(7.8f), fondo));
            tabla.addCell(celdaParte(rolParte(parte.rol()),
                    ModaLinkPdfEstilos.fuente(8, Font.NORMAL), fondo));
            tabla.addCell(celdaParte(estadoFirma(parte.estado()),
                    ModaLinkPdfEstilos.fuente(8, Font.NORMAL), fondo));
            tabla.addCell(celdaParte(fechaFirma(parte),
                    ModaLinkPdfEstilos.fuente(8, Font.NORMAL), fondo));
            indice++;
        }
        return tabla;
    }

    private PdfPCell cabecera(String texto, int alineacion) {
        PdfPCell cell = new PdfPCell(new Phrase(
                9, texto,
                ModaLinkPdfEstilos.fuenteBlanca(7.5f, Font.BOLD)));
        cell.setBackgroundColor(ModaLinkPdfEstilos.TEXTO);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingTop(5);
        cell.setPaddingBottom(5);
        cell.setPaddingLeft(8);
        cell.setPaddingRight(8);
        cell.setHorizontalAlignment(alineacion);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private PdfPCell celdaParte(String texto, Font fuente, Color fondo) {
        PdfPCell cell = new PdfPCell(new Phrase(9, texto, fuente));
        cell.setBackgroundColor(fondo);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(ModaLinkPdfEstilos.BORDE);
        cell.setBorderWidth(0.5f);
        cell.setPaddingTop(5);
        cell.setPaddingBottom(5);
        cell.setPaddingLeft(8);
        cell.setPaddingRight(8);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private void agregarMotivosRechazo(
            Document document, DatosContratoPdf datos) throws DocumentException {

        if (datos.partes() == null) {
            return;
        }
        for (PartePdf parte : datos.partes()) {
            if (parte.motivoRechazo() == null
                    || parte.motivoRechazo().isBlank()) {
                continue;
            }
            Paragraph p = new Paragraph(
                    "Motivo de rechazo — " + valor(parte.nombre()) + ": "
                            + sanitizar(parte.motivoRechazo()),
                    ModaLinkPdfEstilos.fuenteGris(7.8f, Font.NORMAL));
            p.setLeading(10);
            p.setSpacingAfter(2);
            document.add(p);
        }
    }

    private String fechaFirma(PartePdf parte) {
        String estado = parte.estado();
        if ("RECHAZADO".equals(estado) && parte.fechaRechazo() != null) {
            return "Rechazado " + parte.fechaRechazo().format(FECHA_HORA);
        }
        if (parte.fechaFirma() != null) {
            return parte.fechaFirma().format(FECHA_HORA);
        }
        return "—";
    }

    /* ------------------------------------------------------------------ */
    /* Cláusulas                                                          */
    /* ------------------------------------------------------------------ */

    private void agregarClausulas(Document document, String texto)
            throws DocumentException {

        String normalizado = texto(texto)
                .replace("\r\n", "\n")
                .replace('\r', '\n');

        if (normalizado.isBlank()) {
            document.add(new Paragraph(
                    "Sin texto disponible.",
                    ModaLinkPdfEstilos.fuenteGris(9, Font.NORMAL)));
            return;
        }

        boolean anteriorVacia = false;
        for (String linea : normalizado.split("\n", -1)) {
            String limpia = sanitizar(linea.replace('\t', ' '))
                    .stripTrailing();

            if (limpia.isBlank()) {
                if (!anteriorVacia) {
                    Paragraph espacio = new Paragraph(
                            " ", ModaLinkPdfEstilos.fuente(6, Font.NORMAL));
                    espacio.setLeading(6);
                    document.add(espacio);
                    anteriorVacia = true;
                }
                continue;
            }

            anteriorVacia = false;
            boolean titulo = esTitulo(limpia);
            boolean item = ITEM.matcher(limpia).matches();

            Paragraph p = new Paragraph(
                    limpia,
                    titulo
                            ? ModaLinkPdfEstilos.fuente(10.5f, Font.BOLD)
                            : ModaLinkPdfEstilos.fuente(9.5f, Font.NORMAL));
            p.setLeading(titulo ? 14f : 13f);
            p.setSpacingAfter(titulo ? 6f : 3f);
            p.setAlignment(titulo
                    ? Element.ALIGN_LEFT
                    : Element.ALIGN_JUSTIFIED);

            if (item) {
                p.setIndentationLeft(16);
                p.setFirstLineIndent(-16);
            }
            document.add(p);
        }
    }

    /**
     * Considera título a una línea corta, completamente en mayúsculas y sin
     * puntuación de cierre. Es una heurística de presentación: nunca cambia el
     * texto, solo el peso tipográfico.
     */
    private boolean esTitulo(String linea) {
        String t = linea.trim();
        if (t.length() < 4 || t.length() > 110) {
            return false;
        }
        if (!t.equals(t.toUpperCase(Locale.ROOT))) {
            return false;
        }
        if (t.codePoints().noneMatch(Character::isLetter)) {
            return false;
        }
        char ultimo = t.charAt(t.length() - 1);
        return ultimo != '.' && ultimo != ',' && ultimo != ';';
    }

    /* ------------------------------------------------------------------ */
    /* Utilidades                                                         */
    /* ------------------------------------------------------------------ */

    private Paragraph titulo(String texto, float antes, float despues) {
        Paragraph p = new Paragraph(
                texto, ModaLinkPdfEstilos.fuente(11, Font.BOLD));
        p.setLeading(14);
        p.setSpacingBefore(antes);
        p.setSpacingAfter(despues);
        return p;
    }

    private String estadoContrato(String estado) {
        if (estado == null) {
            return "—";
        }
        return switch (estado) {
            case "BORRADOR" -> "Borrador";
            case "PENDIENTE_FIRMA" -> "Pendiente de firma";
            case "ACTIVO" -> "Activo";
            case "RECHAZADO_POR_PARTES" -> "Rechazado por las partes";
            case "CANCELADO" -> "Cancelado";
            case "ELIMINADO" -> "Eliminado";
            default -> estado;
        };
    }

    private Color colorEstado(String estado) {
        if (estado == null) {
            return ModaLinkPdfEstilos.GRIS;
        }
        return switch (estado) {
            case "ACTIVO" -> VERDE;
            case "RECHAZADO_POR_PARTES", "ELIMINADO", "CANCELADO" -> ROJO;
            case "BORRADOR" -> AMBAR;
            default -> ModaLinkPdfEstilos.GRIS;
        };
    }

    private String rolParte(String rol) {
        if (rol == null) {
            return "—";
        }
        return switch (rol) {
            case "DIRECTOR" -> "Director";
            case "DESTINATARIO" -> "Destinatario";
            case "DIRECTOR_DESTINATARIO" -> "Director y destinatario";
            default -> rol;
        };
    }

    private String estadoFirma(String estado) {
        if (estado == null) {
            return "—";
        }
        return switch (estado) {
            case "PENDIENTE" -> "Pendiente";
            case "FIRMADO" -> "Firmado";
            case "RECHAZADO" -> "Rechazado";
            case "CANCELADA" -> "Cancelada";
            case "INVALIDADA" -> "Invalidada";
            default -> estado;
        };
    }

    private String fecha(
            LocalDateTime valor, DateTimeFormatter formato, String vacio) {
        return valor == null ? vacio : valor.format(formato);
    }

    private int version(Integer version) {
        return version == null ? 1 : version;
    }

    private String texto(String valor) {
        return valor == null ? "" : sanitizar(valor);
    }

    private String valor(String valor) {
        return valor == null || valor.isBlank() ? "—" : sanitizar(valor);
    }

    private String sanitizar(String valor) {
        StringBuilder sb = new StringBuilder(valor.length());
        for (int i = 0; i < valor.length(); ) {
            int cp = valor.codePointAt(i);
            if (esRepresentable(cp)) {
                sb.appendCodePoint(cp);
            } else {
                sb.append('?');
            }
            i += Character.charCount(cp);
        }
        return sb.toString();
    }

    private boolean esRepresentable(int cp) {
        if (cp == '\n' || cp == '\t') {
            return true;
        }
        if (cp >= 32 && cp <= 255) {
            return true;
        }
        return switch (cp) {
            case 0x20AC, // euro
                 0x201A, 0x0192, 0x201E, 0x2026, 0x2020, 0x2021,
                 0x02C6, 0x2030, 0x0160, 0x2039, 0x0152, 0x017D,
                 0x2018, 0x2019, 0x201C, 0x201D, 0x2022, 0x2013,
                 0x2014, 0x02DC, 0x2122, 0x0161, 0x203A, 0x0153,
                 0x017E, 0x0178 -> true;
            default -> false;
        };
    }

    /**
     * Datos ya resueltos que necesita el renderer.
     */
    public record DatosContratoPdf(
            Long idContrato,
            String titulo,
            String proyecto,
            String tipoAcuerdo,
            String estado,
            Integer version,
            LocalDateTime fechaCreacion,
            LocalDateTime fechaEmision,
            LocalDateTime fechaModificacion,
            LocalDateTime fechaBaja,
            LocalDateTime fechaCancelacion,
            String motivoCancelacion,
            String texto,
            String emisorNombre,
            List<PartePdf> partes,
            int firmados,
            int totalPartes,
            String exportadoPor,
            LocalDateTime exportadoEn,
            LocalDateTime impresoEn) {
    }

    public record PartePdf(
            String nombre,
            String dni,
            String rol,
            String estado,
            LocalDateTime fechaFirma,
            LocalDateTime fechaRechazo,
            String motivoRechazo) {
    }
}
