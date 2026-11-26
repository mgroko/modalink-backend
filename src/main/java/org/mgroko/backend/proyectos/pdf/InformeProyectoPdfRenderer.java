package org.mgroko.backend.proyectos.pdf;

import java.awt.Color;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.mgroko.backend.common.pdf.ModaLinkPdfEstilos;
import org.mgroko.backend.proyectos.dto.AlertaInforme;
import org.mgroko.backend.proyectos.dto.EstadoAreaInforme;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.ActividadProximaInforme;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.AreaInforme;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.ContratoInforme;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.DecisionInforme;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.HistorialInforme;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.MiembroInforme;
import org.mgroko.backend.proyectos.dto.InformeProyectoResponse.RequerimientoCoberturaInforme;
import org.mgroko.backend.proyectos.dto.SeveridadInforme;
import org.mgroko.backend.proyectos.exception.ErrorGenerandoInformeProyectoException;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Image;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Renderiza el PDF del informe ejecutivo de proyecto.
 */
@Component
public class InformeProyectoPdfRenderer {

    private static final Logger log =
            LoggerFactory.getLogger(InformeProyectoPdfRenderer.class);

    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Color ROJO = new Color(0xC0, 0x39, 0x2B);
    private static final Color ROJO_FONDO = new Color(0xFD, 0xED, 0xEC);
    private static final Color AMARILLO = new Color(0xB7, 0x79, 0x1F);
    private static final Color AMARILLO_FONDO = new Color(0xFE, 0xF5, 0xE7);
    private static final Color VERDE = new Color(0x1E, 0x84, 0x49);
    private static final Color VERDE_FONDO = new Color(0xE9, 0xF7, 0xEF);
    private static final Color GRIS_FONDO = new Color(0xF3, 0xF5, 0xF9);
    private static final Color GRIS_TEXTO = new Color(0x5B, 0x65, 0x77);

    private static final float MARGEN_LATERAL = 36f;
    private static final float MARGEN_SUPERIOR = 44f;
    private static final float MARGEN_INFERIOR = 40f;

    private final PdfImagenResolver imagenResolver;
    private final int maxImagenesPdf;

    public InformeProyectoPdfRenderer(
            PdfImagenResolver imagenResolver,
            @Value("${app.informe-proyecto.max-imagenes-pdf:12}") int maxImagenesPdf) {
        this.imagenResolver = imagenResolver;
        this.maxImagenesPdf = Math.max(1, maxImagenesPdf);
    }

    public void render(InformeProyectoResponse datos, OutputStream salida) {
        Document document = new Document(
                PageSize.A4,
                MARGEN_LATERAL,
                MARGEN_LATERAL,
                MARGEN_SUPERIOR,
                MARGEN_INFERIOR);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, salida);
            writer.setPageEvent(new InformeProyectoPdfPageEvent(
                    datos.impresoEn(),
                    datos.generadoEn(),
                    datos.proyecto().nombre(),
                    datos.generadoPor() != null
                            ? datos.generadoPor().nombreArtistico()
                            : null));

            document.addTitle("Informe de proyecto · ModaLink");
            document.addAuthor("ModaLink");
            document.addSubject("Informe ejecutivo de estado y salud del proyecto");
            document.addCreator("ModaLink");

            document.open();

            Map<String, Optional<Image>> cacheImagenes = new HashMap<>();
            addPortadaYResumen(document, datos);
            document.newPage();
            addMoodboardYEquipo(document, datos, cacheImagenes);
            document.newPage();
            addCronogramaYContratos(document, datos);
            document.newPage();
            addAlertasYCierre(document, datos);

        } catch (DocumentException ex) {
            log.error("No se pudo generar el PDF del informe del proyecto {}",
                    datos != null && datos.proyecto() != null
                            ? datos.proyecto().idProyecto()
                            : null,
                    ex);
            throw new ErrorGenerandoInformeProyectoException(
                    "No se pudo generar el informe del proyecto.", ex);
        } finally {
            if (document.isOpen()) {
                try {
                    document.close();
                } catch (RuntimeException closeEx) {
                    log.error("Error cerrando el documento PDF del informe de proyecto.", closeEx);
                }
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /* Página 1: portada, estado por área, riesgo y decisiones            */
    /* ------------------------------------------------------------------ */

    private void addPortadaYResumen(Document document, InformeProyectoResponse datos)
            throws DocumentException {

        Paragraph eyebrow = new Paragraph(
                "INFORME EJECUTIVO DE ESTADO Y SALUD",
                ModaLinkPdfEstilos.fuenteGris(8, Font.BOLD));
        eyebrow.setSpacingAfter(6);
        document.add(eyebrow);

        Paragraph nombre = new Paragraph(
                texto(datos.proyecto().nombre(), "Proyecto"),
                ModaLinkPdfEstilos.fuente(24, Font.BOLD));
        nombre.setLeading(28);
        nombre.setSpacingAfter(4);
        document.add(nombre);

        document.add(badges(datos));
        document.add(descripcionProyecto(datos));
        document.add(fichaProyecto(datos));
        document.add(metadatosDocumento(datos));

        document.add(titulo("Estado por área"));
        document.add(estadoGeneral(datos));
        document.add(tarjetasAreas(datos.resumen().areas()));

        document.add(titulo("Riesgo para la fecha de entrega"));
        document.add(riesgoEntrega(datos.resumen().riesgoEntrega()));

        document.add(titulo("Requiere tu decisión"));
        document.add(tablaDecisiones(datos.resumen().decisiones()));
    }

    private PdfPTable badges(InformeProyectoResponse datos) {
        PdfPTable tabla = new PdfPTable(new float[]{1.2f, 1.2f, 1.2f});
        tabla.setWidthPercentage(62);
        tabla.setHorizontalAlignment(Element.ALIGN_LEFT);
        tabla.setSpacingAfter(8);

        tabla.addCell(badge(texto(datos.proyecto().estado(), "—"), GRIS_FONDO, GRIS_TEXTO));
        tabla.addCell(badge("MODO " + texto(datos.modo(), "—"), GRIS_FONDO, GRIS_TEXTO));
        tabla.addCell(badge("Proyecto " + texto(datos.proyecto().privacidad(), "—"),
                GRIS_FONDO, GRIS_TEXTO));
        return tabla;
    }

    private Paragraph descripcionProyecto(InformeProyectoResponse datos) {
        Paragraph descripcion = new Paragraph(
                texto(datos.proyecto().descripcion(), "Sin descripción."),
                ModaLinkPdfEstilos.fuenteGris(10, Font.NORMAL));
        descripcion.setLeading(14);
        descripcion.setSpacingAfter(12);
        return descripcion;
    }

    private PdfPTable fichaProyecto(InformeProyectoResponse datos) {
        PdfPTable tabla = new PdfPTable(new float[]{1f, 1f, 1f, 1.4f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingAfter(8);

        tabla.addCell(ficha(
                "Director",
                datos.generadoPor() != null
                        ? datos.generadoPor().nombreArtistico()
                        : "—"));
        tabla.addCell(ficha("Inicio", fecha(datos.proyecto().fechaInicio())));
        tabla.addCell(ficha("Entrega", fecha(datos.proyecto().fechaEntrega())));
        tabla.addCell(ficha("Ubicación", ubicacion(datos)));
        return tabla;
    }

    private PdfPTable metadatosDocumento(InformeProyectoResponse datos) {
        PdfPTable tabla = new PdfPTable(new float[]{1f, 1f, 1f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingAfter(18);

        tabla.addCell(meta("PROYECTO", "#" + datos.proyecto().idProyecto()));
        tabla.addCell(meta("CORTE", fechaHora(datos.generadoEn())));
        tabla.addCell(meta("GENERADO POR", datos.generadoPor() != null
                ? datos.generadoPor().nombreArtistico()
                : "—"));
        tabla.addCell(meta("MODO", texto(datos.modo(), "—")));
        tabla.addCell(meta("VERSIÓN", texto(datos.versionInforme(), "—")));
        tabla.addCell(meta("SISTEMA", "ModaLink"));
        return tabla;
    }

    private PdfPTable estadoGeneral(InformeProyectoResponse datos) {
        EstadoAreaInforme estado = estado(datos.resumen().estadoGeneral());
        PdfPTable tabla = new PdfPTable(new float[]{3f, 1f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingAfter(10);

        PdfPCell izquierda = new PdfPCell(new Phrase(
                "Estado general: el peor de las áreas",
                ModaLinkPdfEstilos.fuente(9, Font.NORMAL)));
        izquierda.setBackgroundColor(GRIS_FONDO);
        izquierda.setBorderColor(ModaLinkPdfEstilos.BORDE);
        izquierda.setPadding(8);
        izquierda.setVerticalAlignment(Element.ALIGN_MIDDLE);

        PdfPCell derecha = new PdfPCell();
        derecha.setBackgroundColor(fondoEstado(estado));
        derecha.setBorderColor(colorEstado(estado));
        derecha.setPadding(6);
        derecha.addElement(new Paragraph(
                estado.name() + (estado == EstadoAreaInforme.ROJO ? " · requiere atención" : ""),
                fuenteColor(colorEstado(estado), 9, Font.BOLD)));

        tabla.addCell(izquierda);
        tabla.addCell(derecha);
        return tabla;
    }

    private PdfPTable tarjetasAreas(List<AreaInforme> areas) {
        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        tabla.setSpacingAfter(18);

        for (AreaInforme area : areas) {
            PdfPCell celda = new PdfPCell();
            celda.setBackgroundColor(fondoEstado(area.estado()));
            celda.setBorderColor(colorEstado(area.estado()));
            celda.setBorderWidth(0.7f);
            celda.setPadding(10);
            celda.setMinimumHeight(58f);

            PdfPTable cabecera = new PdfPTable(new float[]{2f, 1f});
            cabecera.setWidthPercentage(100);
            cabecera.addCell(sinBorde(new Paragraph(
                    nombreArea(area.codigo()),
                    ModaLinkPdfEstilos.fuente(11, Font.BOLD))));
            cabecera.addCell(sinBorde(new Paragraph(
                    area.estado().name(),
                    fuenteColor(colorEstado(area.estado()), 8, Font.BOLD))));

            celda.addElement(cabecera);
            Paragraph motivo = new Paragraph(
                    area.motivo(),
                    ModaLinkPdfEstilos.fuente(8.5f, Font.NORMAL));
            motivo.setLeading(11);
            celda.addElement(motivo);
            tabla.addCell(celda);
        }

        if (areas.size() % 2 != 0) {
            tabla.addCell(new PdfPCell(new Phrase("")));
        }
        return tabla;
    }

    private PdfPTable riesgoEntrega(InformeProyectoResponse.RiesgoEntregaInforme riesgo) {
        PdfPTable tabla = new PdfPTable(new float[]{1f, 1f, 1f, 1f, 1f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingAfter(18);

        tabla.addCell(kpi("Fin proyectado", fecha(riesgo.fechaFinProyectada())));
        tabla.addCell(kpi("Entrega", fecha(riesgo.fechaEntrega())));
        tabla.addCell(kpi("Margen", dias(riesgo.margenDias())));
        tabla.addCell(kpi("Críticas sin iniciar", String.valueOf(riesgo.criticasSinIniciar())));
        tabla.addCell(kpi("Avance", porcentaje(riesgo.avanceActividades())));

        if (riesgo.sinDatos()) {
            PdfPCell aviso = new PdfPCell(new Phrase(
                    "Sin datos suficientes para calcular el riesgo de entrega.",
                    ModaLinkPdfEstilos.fuenteGris(8, Font.NORMAL)));
            aviso.setColspan(5);
            aviso.setBorder(Rectangle.NO_BORDER);
            aviso.setPaddingTop(6);
            tabla.addCell(aviso);
        }
        return tabla;
    }

    private PdfPTable tablaDecisiones(List<DecisionInforme> decisiones) {
        PdfPTable tabla = new PdfPTable(new float[]{0.8f, 3.4f, 1f, 2.1f});
        tabla.setWidthPercentage(100);
        tabla.setHeaderRows(1);

        tabla.addCell(header("Prioridad"));
        tabla.addCell(header("Qué pasa"));
        tabla.addCell(header("Antigüedad"));
        tabla.addCell(header("Acción"));

        if (decisiones.isEmpty()) {
            PdfPCell vacia = new PdfPCell(new Phrase(
                    "Sin acciones críticas en este momento.",
                    ModaLinkPdfEstilos.fuenteGris(9, Font.NORMAL)));
            vacia.setColspan(4);
            vacia.setPadding(10);
            vacia.setBorderColor(ModaLinkPdfEstilos.BORDE);
            tabla.addCell(vacia);
            return tabla;
        }

        for (DecisionInforme decision : decisiones) {
            SeveridadInforme severidad = severidad(decision.prioridad());
            PdfPCell prioridad = new PdfPCell(new Phrase(
                    decision.prioridad(),
                    fuenteColor(colorSeveridad(severidad), 8, Font.BOLD)));
            prioridad.setBackgroundColor(fondoSeveridad(severidad));
            prioridad.setPadding(6);
            prioridad.setBorderColor(ModaLinkPdfEstilos.BORDE);
            tabla.addCell(prioridad);

            tabla.addCell(celda(decision.mensaje(), 8.5f));
            tabla.addCell(celda(antiguedad(decision.antiguedadDias()), 8.5f));
            tabla.addCell(celda(decision.accionSugerida(), 8.5f));
        }
        return tabla;
    }

    /* ------------------------------------------------------------------ */
    /* Página 2: moodboard, equipo, cobertura y miembros                   */
    /* ------------------------------------------------------------------ */

    private void addMoodboardYEquipo(
            Document document,
            InformeProyectoResponse datos,
            Map<String, Optional<Image>> cacheImagenes) throws DocumentException {

        document.add(titulo("Moodboard"));
        document.add(descripcionMoodboard(datos));
        document.add(grillaMoodboard(datos, cacheImagenes));

        document.add(titulo("Equipo"));
        document.add(kpisEquipo(datos));
        document.add(tablaCobertura(datos.equipo().requerimientos()));
        document.add(tablaMiembros(datos.equipo().miembros()));
        document.add(notaPrivacidadMiembros());
    }

    private Paragraph descripcionMoodboard(InformeProyectoResponse datos) {
        var moodboard = datos.moodboard();
        String descripcion = moodboard.descripcion() == null
                || moodboard.descripcion().isBlank()
                        ? "Sin descripción conceptual."
                        : moodboard.descripcion();
        String detalle = moodboard.totalImagenes() + " de "
                + moodboard.limiteConfigurado() + " imágenes"
                + (moodboard.fechaCreacion() != null
                        ? " · creado el " + fechaHora(moodboard.fechaCreacion())
                        : "");
        Paragraph p = new Paragraph(
                descripcion + " · " + detalle,
                ModaLinkPdfEstilos.fuenteGris(8.5f, Font.NORMAL));
        p.setSpacingAfter(8);
        return p;
    }

    private PdfPTable grillaMoodboard(
            InformeProyectoResponse datos,
            Map<String, Optional<Image>> cacheImagenes) {

        List<InformeProyectoResponse.ImagenInforme> imagenes =
                datos.moodboard().imagenes().stream()
                        .limit(maxImagenesPdf)
                        .toList();

        PdfPTable tabla = new PdfPTable(4);
        tabla.setWidthPercentage(100);
        tabla.setSpacingAfter(16);

        if (imagenes.isEmpty()) {
            PdfPCell vacia = new PdfPCell(new Phrase(
                    "Sin imágenes de moodboard.",
                    ModaLinkPdfEstilos.fuenteGris(9, Font.NORMAL)));
            vacia.setColspan(4);
            vacia.setPadding(20);
            vacia.setHorizontalAlignment(Element.ALIGN_CENTER);
            vacia.setBackgroundColor(GRIS_FONDO);
            vacia.setBorderColor(ModaLinkPdfEstilos.BORDE);
            tabla.addCell(vacia);
            return tabla;
        }

        for (var imagen : imagenes) {
            tabla.addCell(celdaImagen(imagen, cacheImagenes));
        }

        int faltantes = 4 - (imagenes.size() % 4);
        if (faltantes < 4) {
            for (int i = 0; i < faltantes; i++) {
                PdfPCell vacia = new PdfPCell(new Phrase(""));
                vacia.setBorder(Rectangle.NO_BORDER);
                vacia.setFixedHeight(92f);
                tabla.addCell(vacia);
            }
        }

        if (datos.moodboard().totalImagenes() > maxImagenesPdf) {
            PdfPCell nota = new PdfPCell(new Phrase(
                    (datos.moodboard().totalImagenes() - maxImagenesPdf)
                            + " imagen(es) adicional(es) no incluidas en el PDF.",
                    ModaLinkPdfEstilos.fuenteGris(8, Font.NORMAL)));
            nota.setColspan(4);
            nota.setBorder(Rectangle.NO_BORDER);
            nota.setPaddingTop(4);
            tabla.addCell(nota);
        }
        return tabla;
    }

    private PdfPCell celdaImagen(
            InformeProyectoResponse.ImagenInforme imagen,
            Map<String, Optional<Image>> cacheImagenes) {

        PdfPCell celda = new PdfPCell();
        celda.setBackgroundColor(GRIS_FONDO);
        celda.setBorderColor(ModaLinkPdfEstilos.BORDE);
        celda.setPadding(4);
        celda.setFixedHeight(92f);

        Optional<Image> pdfImagen = cacheImagenes.computeIfAbsent(
                imagen.url(),
                imagenResolver::resolver);

        if (pdfImagen.isPresent()) {
            Image img = pdfImagen.get();
            img.scaleToFit(150, 72);
            img.setAlignment(Element.ALIGN_CENTER);
            celda.addElement(img);
        } else {
            Paragraph noDisponible = new Paragraph(
                    "Imagen no disponible",
                    ModaLinkPdfEstilos.fuenteGris(8, Font.NORMAL));
            noDisponible.setAlignment(Element.ALIGN_CENTER);
            celda.addElement(noDisponible);
        }

        Paragraph pie = new Paragraph(
                "#" + imagen.idImagen(),
                ModaLinkPdfEstilos.fuenteGris(7, Font.NORMAL));
        pie.setAlignment(Element.ALIGN_CENTER);
        celda.addElement(pie);
        return celda;
    }

    private PdfPTable kpisEquipo(InformeProyectoResponse datos) {
        PdfPTable tabla = new PdfPTable(new float[]{1f, 1f, 1f, 1f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingAfter(12);

        tabla.addCell(kpi("Miembros activos", String.valueOf(datos.equipo().totalActivos())));
        tabla.addCell(kpi("Directores", String.valueOf(datos.equipo().totalDirectores())));
        tabla.addCell(kpi("Bajas", String.valueOf(datos.equipo().totalBajas())));
        tabla.addCell(kpi("Estabilidad", porcentaje(datos.equipo().estabilidad())));
        return tabla;
    }

    private PdfPTable tablaCobertura(List<RequerimientoCoberturaInforme> requerimientos) {
        PdfPTable tabla = new PdfPTable(new float[]{2.8f, 1f, 1f, 1.4f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingAfter(14);
        tabla.setHeaderRows(1);

        tabla.addCell(header("Requerimiento"));
        tabla.addCell(header("Cubiertos"));
        tabla.addCell(header("Faltan"));
        tabla.addCell(header("Cobertura"));

        if (requerimientos.isEmpty()) {
            PdfPCell vacia = new PdfPCell(new Phrase(
                    "Sin requerimientos activos.",
                    ModaLinkPdfEstilos.fuenteGris(9, Font.NORMAL)));
            vacia.setColspan(4);
            vacia.setPadding(10);
            vacia.setBorderColor(ModaLinkPdfEstilos.BORDE);
            tabla.addCell(vacia);
            return tabla;
        }

        for (RequerimientoCoberturaInforme requerimiento : requerimientos) {
            tabla.addCell(celda(requerimiento.profesion(), 8.5f));
            tabla.addCell(celda(requerimiento.cubiertos() + " de " + requerimiento.cantidad(), 8.5f));
            tabla.addCell(celda(String.valueOf(requerimiento.faltantes()), 8.5f));
            tabla.addCell(celdaBarra(requerimiento.porcentajeCobertura()));
        }
        return tabla;
    }

    private PdfPTable tablaMiembros(List<MiembroInforme> miembros) {
        PdfPTable tabla = new PdfPTable(new float[]{2.2f, 1.5f, 1f, 1f, 1f, 0.8f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingAfter(4);
        tabla.setHeaderRows(1);

        tabla.addCell(header("Nombre artístico"));
        tabla.addCell(header("Profesión"));
        tabla.addCell(header("Rol"));
        tabla.addCell(header("Estado"));
        tabla.addCell(header("Alta"));
        tabla.addCell(header("T&C"));

        if (miembros.isEmpty()) {
            PdfPCell vacia = new PdfPCell(new Phrase(
                    "Sin miembros registrados.",
                    ModaLinkPdfEstilos.fuenteGris(9, Font.NORMAL)));
            vacia.setColspan(6);
            vacia.setPadding(10);
            vacia.setBorderColor(ModaLinkPdfEstilos.BORDE);
            tabla.addCell(vacia);
            return tabla;
        }

        for (MiembroInforme miembro : miembros) {
            tabla.addCell(celda(miembro.nombreArtistico(), 8.5f));
            tabla.addCell(celda(miembro.profesion(), 8.5f));
            tabla.addCell(celda(miembro.rol(), 8f));
            tabla.addCell(celdaEstadoMiembro(miembro.estadoParticipacion()));
            tabla.addCell(celda(fecha(miembro.fechaAlta()), 8f));
            tabla.addCell(celda(Boolean.TRUE.equals(miembro.tycAceptado()) ? "Sí" : "Pendiente", 8f));
        }
        return tabla;
    }

    private Paragraph notaPrivacidadMiembros() {
        Paragraph p = new Paragraph(
                "Se muestran solo nombre artístico, profesión, rol, estado y fechas. "
                        + "No se incluyen DNI, correo ni teléfono.",
                ModaLinkPdfEstilos.fuenteGris(7.5f, Font.NORMAL));
        p.setSpacingAfter(8);
        return p;
    }

    /* ------------------------------------------------------------------ */
    /* Página 3: cronograma, actividades y contratos                       */
    /* ------------------------------------------------------------------ */

    private void addCronogramaYContratos(Document document, InformeProyectoResponse datos)
            throws DocumentException {

        var cronograma = datos.cronograma();
        document.add(titulo("Cronograma"));
        document.add(kpisCronograma(cronograma));
        document.add(lineaTiempo(cronograma));
        document.add(titulo("Actividades"));
        document.add(kpisActividades(cronograma));
        document.add(barraEstados(cronograma.porEstado()));
        document.add(tablaProximasActividades(cronograma.proximasActividades()));

        document.add(titulo("Contratos"));
        document.add(kpisContratos(datos.contratos()));
        document.add(tablaContratos(datos.contratos().contratos()));
        document.add(new Paragraph(
                "No se incluye el texto de los contratos.",
                ModaLinkPdfEstilos.fuenteGris(7.5f, Font.NORMAL)));
    }

    private PdfPTable kpisCronograma(InformeProyectoResponse.CronogramaInforme cronograma) {
        PdfPTable tabla = new PdfPTable(new float[]{1f, 1f, 1f, 1f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingAfter(10);

        tabla.addCell(kpi("Fin proyectado", fecha(cronograma.fechaFinProyectada())));
        tabla.addCell(kpi("Entrega", fecha(cronograma.fechaEntrega())));
        tabla.addCell(kpi("Margen", dias(cronograma.margenDias())));
        tabla.addCell(kpi("Estado", texto(cronograma.estadoCronograma(), "Sin datos")));
        return tabla;
    }

    private PdfPTable lineaTiempo(InformeProyectoResponse.CronogramaInforme cronograma) {
        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        tabla.setSpacingAfter(16);

        PdfPCell linea = new PdfPCell(new Phrase(
                "Inicio " + fechaHora(cronograma.fechaInicio())
                        + "   →   Fin proyectado " + fechaHora(cronograma.fechaFinProyectada()),
                ModaLinkPdfEstilos.fuenteBlanca(8, Font.BOLD)));
        linea.setBackgroundColor(ModaLinkPdfEstilos.AZUL);
        linea.setBorder(Rectangle.NO_BORDER);
        linea.setPadding(8);

        PdfPCell entrega = new PdfPCell(new Phrase(
                "Entrega " + fecha(cronograma.fechaEntrega()),
                ModaLinkPdfEstilos.fuente(8, Font.BOLD)));
        entrega.setBackgroundColor(GRIS_FONDO);
        entrega.setBorderColor(ModaLinkPdfEstilos.BORDE);
        entrega.setPadding(8);

        tabla.addCell(linea);
        tabla.addCell(entrega);
        return tabla;
    }

    private PdfPTable kpisActividades(InformeProyectoResponse.CronogramaInforme cronograma) {
        PdfPTable tabla = new PdfPTable(new float[]{1f, 1f, 1f, 1f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingAfter(8);

        tabla.addCell(kpi("Total", String.valueOf(cronograma.totalActividades())));
        tabla.addCell(kpi("Críticas", String.valueOf(cronograma.actividadesCriticas())));
        tabla.addCell(kpi("Vencidas", String.valueOf(cronograma.actividadesVencidas())));
        tabla.addCell(kpi("Conflictos", String.valueOf(cronograma.conflictos())));
        return tabla;
    }

    private PdfPTable barraEstados(Map<String, Integer> porEstado) {
        int pendientes = porEstado.getOrDefault("PENDIENTE", 0);
        int enCurso = porEstado.getOrDefault("EN_CURSO", 0);
        int finalizadas = porEstado.getOrDefault("FINALIZADA", 0);
        int canceladas = porEstado.getOrDefault("CANCELADA", 0);
        int total = pendientes + enCurso + finalizadas + canceladas;

        PdfPTable tabla = new PdfPTable(new float[]{
                Math.max(0.01f, pendientes),
                Math.max(0.01f, enCurso),
                Math.max(0.01f, finalizadas),
                Math.max(0.01f, canceladas)});
        tabla.setWidthPercentage(100);
        tabla.setSpacingAfter(8);

        tabla.addCell(celdaBarra(new Color(0xD9, 0xDE, 0xE7)));
        tabla.addCell(celdaBarra(new Color(0xF0, 0xA8, 0x3C)));
        tabla.addCell(celdaBarra(VERDE));
        tabla.addCell(celdaBarra(new Color(0xC9, 0xD0, 0xDA)));

        Paragraph leyenda = new Paragraph(
                "Pendientes " + pendientes + "   ·   En curso " + enCurso
                        + "   ·   Finalizadas " + finalizadas
                        + "   ·   Canceladas " + canceladas
                        + "   ·   Total " + total,
                ModaLinkPdfEstilos.fuenteGris(8, Font.NORMAL));
        leyenda.setSpacingAfter(12);

        PdfPTable contenedor = new PdfPTable(1);
        contenedor.setWidthPercentage(100);
        PdfPCell celda = new PdfPCell();
        celda.setBorder(Rectangle.NO_BORDER);
        celda.addElement(tabla);
        celda.addElement(leyenda);
        contenedor.addCell(celda);
        return contenedor;
    }

    private PdfPTable tablaProximasActividades(List<ActividadProximaInforme> actividades) {
        PdfPTable tabla = new PdfPTable(new float[]{2.8f, 1.2f, 1f, 0.9f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingAfter(16);
        tabla.setHeaderRows(1);

        tabla.addCell(header("Próximas actividades"));
        tabla.addCell(header("Inicio"));
        tabla.addCell(header("Estado"));
        tabla.addCell(header("Ruta crítica"));

        if (actividades.isEmpty()) {
            PdfPCell vacia = new PdfPCell(new Phrase(
                    "Sin próximas actividades programadas.",
                    ModaLinkPdfEstilos.fuenteGris(9, Font.NORMAL)));
            vacia.setColspan(4);
            vacia.setPadding(10);
            vacia.setBorderColor(ModaLinkPdfEstilos.BORDE);
            tabla.addCell(vacia);
            return tabla;
        }

        for (ActividadProximaInforme actividad : actividades) {
            tabla.addCell(celda(actividad.nombre(), 8.5f));
            tabla.addCell(celda(fechaHora(actividad.fechaHoraInicio()), 8.5f));
            tabla.addCell(celda(actividad.estado(), 8f));
            tabla.addCell(celda(Boolean.TRUE.equals(actividad.enRutaCritica()) ? "Sí" : "No", 8f));
        }
        return tabla;
    }

    private PdfPTable kpisContratos(InformeProyectoResponse.ContratosInforme contratos) {
        int activos = contratos.porEstado().getOrDefault("ACTIVO", 0);
        int pendientes = contratos.porEstado().getOrDefault("PENDIENTE_FIRMA", 0);

        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        tabla.setSpacingAfter(10);

        tabla.addCell(kpi("Contratos",
                String.valueOf(contratos.total()),
                activos + " activos · " + pendientes + " pendientes de firma"));

        PdfPCell firmas = new PdfPCell();
        firmas.setBackgroundColor(GRIS_FONDO);
        firmas.setBorderColor(ModaLinkPdfEstilos.BORDE);
        firmas.setPadding(8);
        firmas.addElement(new Paragraph(
                "Firmas: " + contratos.firmas().firmadas()
                        + " de " + contratos.firmas().total(),
                ModaLinkPdfEstilos.fuente(12, Font.BOLD)));
        firmas.addElement(barraFirmas(
                contratos.firmas().firmadas(),
                contratos.firmas().total()));
        firmas.addElement(new Paragraph(
                contratos.firmas().pendientes() + " firma(s) pendiente(s)",
                ModaLinkPdfEstilos.fuenteGris(8, Font.NORMAL)));
        tabla.addCell(firmas);

        return tabla;
    }

    private PdfPTable tablaContratos(List<ContratoInforme> contratos) {
        PdfPTable tabla = new PdfPTable(new float[]{3.2f, 0.8f, 1.2f, 0.9f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingAfter(6);
        tabla.setHeaderRows(1);

        tabla.addCell(header("Contrato"));
        tabla.addCell(header("Versión"));
        tabla.addCell(header("Estado"));
        tabla.addCell(header("Firmas"));

        if (contratos.isEmpty()) {
            PdfPCell vacia = new PdfPCell(new Phrase(
                    "Sin contratos registrados.",
                    ModaLinkPdfEstilos.fuenteGris(9, Font.NORMAL)));
            vacia.setColspan(4);
            vacia.setPadding(10);
            vacia.setBorderColor(ModaLinkPdfEstilos.BORDE);
            tabla.addCell(vacia);
            return tabla;
        }

        for (ContratoInforme contrato : contratos) {
            tabla.addCell(celda(contrato.nombre(), 8.5f));
            tabla.addCell(celda("v" + contrato.version(), 8.5f));
            tabla.addCell(celda(contrato.estado(), 8f));
            tabla.addCell(celda(contrato.firmadas() + " de " + contrato.totalPartes(), 8f));
        }
        return tabla;
    }

    /* ------------------------------------------------------------------ */
    /* Página 4: alertas, historial, calificaciones y guía                 */
    /* ------------------------------------------------------------------ */

    private void addAlertasYCierre(Document document, InformeProyectoResponse datos)
            throws DocumentException {

        document.add(titulo("Alertas priorizadas"));
        document.add(tablaAlertas(datos.alertas()));

        document.add(titulo("Historial del equipo"));
        document.add(tablaHistorial(datos.historial()));

        document.add(titulo("Calificaciones"));
        document.add(bloqueCalificaciones(datos.calificaciones()));

        document.add(titulo("Cómo leer este informe"));
        document.add(bloqueAyuda());
    }

    private PdfPTable tablaAlertas(List<AlertaInforme> alertas) {
        PdfPTable tabla = new PdfPTable(new float[]{0.9f, 3.6f, 0.9f, 1.9f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingAfter(16);
        tabla.setHeaderRows(1);

        tabla.addCell(header("Severidad"));
        tabla.addCell(header("Alerta"));
        tabla.addCell(header("Antigüedad"));
        tabla.addCell(header("Acción sugerida"));

        if (alertas.isEmpty()) {
            PdfPCell vacia = new PdfPCell(new Phrase(
                    "Sin alertas para el período.",
                    ModaLinkPdfEstilos.fuenteGris(9, Font.NORMAL)));
            vacia.setColspan(4);
            vacia.setPadding(10);
            vacia.setBorderColor(ModaLinkPdfEstilos.BORDE);
            tabla.addCell(vacia);
            return tabla;
        }

        for (AlertaInforme alerta : alertas) {
            PdfPCell severidad = new PdfPCell(new Phrase(
                    alerta.severidad().name(),
                    fuenteColor(colorSeveridad(alerta.severidad()), 8, Font.BOLD)));
            severidad.setBackgroundColor(fondoSeveridad(alerta.severidad()));
            severidad.setPadding(6);
            severidad.setBorderColor(ModaLinkPdfEstilos.BORDE);
            tabla.addCell(severidad);

            PdfPCell detalle = new PdfPCell();
            detalle.setPadding(6);
            detalle.setBorderColor(ModaLinkPdfEstilos.BORDE);
            detalle.addElement(new Paragraph(
                    alerta.codigo(),
                    ModaLinkPdfEstilos.fuente(8.5f, Font.BOLD)));
            Paragraph mensaje = new Paragraph(
                    alerta.mensaje(),
                    ModaLinkPdfEstilos.fuente(8, Font.NORMAL));
            mensaje.setLeading(10);
            detalle.addElement(mensaje);
            if (alerta.area() != null) {
                detalle.addElement(new Paragraph(
                        "Área: " + nombreArea(alerta.area()),
                        ModaLinkPdfEstilos.fuenteGris(7, Font.NORMAL)));
            }
            tabla.addCell(detalle);

            tabla.addCell(celda(antiguedad(alerta.antiguedadDias()), 8f));
            tabla.addCell(celda(alerta.accionSugerida(), 8f));
        }
        return tabla;
    }

    private PdfPTable tablaHistorial(List<HistorialInforme> historial) {
        PdfPTable tabla = new PdfPTable(new float[]{1f, 1.4f, 1.7f, 2f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingAfter(16);
        tabla.setHeaderRows(1);

        tabla.addCell(header("Fecha"));
        tabla.addCell(header("Evento"));
        tabla.addCell(header("Perfil"));
        tabla.addCell(header("Motivo"));

        if (historial.isEmpty()) {
            PdfPCell vacia = new PdfPCell(new Phrase(
                    "Sin bajas ni eliminaciones registradas.",
                    ModaLinkPdfEstilos.fuenteGris(9, Font.NORMAL)));
            vacia.setColspan(4);
            vacia.setPadding(10);
            vacia.setBorderColor(ModaLinkPdfEstilos.BORDE);
            tabla.addCell(vacia);
            return tabla;
        }

        for (HistorialInforme evento : historial) {
            tabla.addCell(celda(fecha(evento.fecha()), 8.5f));
            tabla.addCell(celda(evento.tipo(), 8.5f));
            tabla.addCell(celda(evento.perfil(), 8.5f));
            tabla.addCell(celda(evento.motivo(), 8.5f));
        }
        return tabla;
    }

    private PdfPTable bloqueCalificaciones(InformeProyectoResponse.CalificacionesInforme calificaciones) {
        PdfPTable tabla = new PdfPTable(1);
        tabla.setWidthPercentage(100);
        tabla.setSpacingAfter(16);

        PdfPCell celda = new PdfPCell();
        celda.setBackgroundColor(GRIS_FONDO);
        celda.setBorderColor(ModaLinkPdfEstilos.BORDE);
        celda.setPadding(10);

        if (!Boolean.TRUE.equals(calificaciones.disponible())) {
            celda.addElement(new Paragraph(
                    "SIN DATOS",
                    ModaLinkPdfEstilos.fuenteGris(9, Font.BOLD)));
            celda.addElement(new Paragraph(
                    "Las calificaciones se habilitan al finalizar el proyecto. "
                            + "Solo se muestran promedio y cantidad, nunca reseñas de terceros.",
                    ModaLinkPdfEstilos.fuenteGris(8, Font.NORMAL)));
        } else {
            celda.addElement(new Paragraph(
                    "Promedio: " + (calificaciones.promedio() != null
                            ? String.format("%.1f", calificaciones.promedio())
                            : "—"),
                    ModaLinkPdfEstilos.fuente(12, Font.BOLD)));
            celda.addElement(new Paragraph(
                    calificaciones.cantidad() + " de " + calificaciones.totalEsperado()
                            + " calificaciones esperadas",
                    ModaLinkPdfEstilos.fuenteGris(8, Font.NORMAL)));
        }

        tabla.addCell(celda);
        return tabla;
    }

    private PdfPTable bloqueAyuda() {
        PdfPTable tabla = new PdfPTable(1);
        tabla.setWidthPercentage(100);
        PdfPCell celda = new PdfPCell();
        celda.setBackgroundColor(GRIS_FONDO);
        celda.setBorderColor(ModaLinkPdfEstilos.BORDE);
        celda.setPadding(10);
        Paragraph texto = new Paragraph(
                "Cada área toma el color de su alerta más grave: rojo si tiene una alerta alta, "
                        + "amarillo si tiene una media y verde si no tiene alertas. "
                        + "El estado general es el área más grave; no se promedia. "
                        + "Un área sin datos se muestra como “Sin datos”, nunca como verde. "
                        + "Este informe es una foto del momento y no reemplaza la auditoría.",
                ModaLinkPdfEstilos.fuente(8.5f, Font.NORMAL));
        texto.setLeading(11);
        celda.addElement(texto);
        tabla.addCell(celda);
        return tabla;
    }

    /* ------------------------------------------------------------------ */
    /* Helpers visuales                                                   */
    /* ------------------------------------------------------------------ */

    private Paragraph titulo(String texto) {
        Paragraph titulo = new Paragraph(
                texto,
                ModaLinkPdfEstilos.fuenteAzul(13, Font.BOLD));
        titulo.setSpacingBefore(8);
        titulo.setSpacingAfter(6);
        return titulo;
    }

    private PdfPCell header(String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(
                texto,
                ModaLinkPdfEstilos.fuenteBlanca(8, Font.BOLD)));
        cell.setBackgroundColor(ModaLinkPdfEstilos.AZUL);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(6);
        return cell;
    }

    private PdfPCell celda(String texto, float tamanio) {
        PdfPCell cell = new PdfPCell(new Phrase(
                texto(texto, "—"),
                ModaLinkPdfEstilos.fuente(tamanio, Font.NORMAL)));
        cell.setBorderColor(ModaLinkPdfEstilos.BORDE);
        cell.setPadding(6);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private PdfPCell celdaEstadoMiembro(String estado) {
        Color color = switch (estado == null ? "" : estado) {
            case "ACTIVO" -> VERDE;
            case "BAJA_VOLUNTARIA", "ELIMINADO" -> ROJO;
            default -> GRIS_TEXTO;
        };
        PdfPCell cell = new PdfPCell(new Phrase(
                texto(estado, "—"),
                fuenteColor(color, 8, Font.BOLD)));
        cell.setBorderColor(ModaLinkPdfEstilos.BORDE);
        cell.setPadding(6);
        return cell;
    }

    private PdfPCell celdaBarra(Double porcentaje) {
        double valor = porcentaje == null ? 0 : Math.max(0, Math.min(1, porcentaje));
        PdfPTable barra = new PdfPTable(new float[]{
                (float) Math.max(0.01, valor),
                (float) Math.max(0.01, 1 - valor)});
        barra.setWidthPercentage(100);
        barra.addCell(celdaBarra(valor >= 0.999 ? VERDE : valor >= 0.5
                ? new Color(0xE6, 0x9B, 0x2E)
                : ROJO));
        barra.addCell(celdaBarra(new Color(0xE9, 0xEC, 0xF2)));

        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(ModaLinkPdfEstilos.BORDE);
        cell.setPadding(6);
        cell.addElement(barra);
        cell.addElement(new Paragraph(
                porcentaje(porcentaje),
                ModaLinkPdfEstilos.fuenteGris(7.5f, Font.NORMAL)));
        return cell;
    }

    private PdfPCell celdaBarra(Color color) {
        PdfPCell cell = new PdfPCell(new Phrase(" "));
        cell.setBackgroundColor(color);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setFixedHeight(8f);
        return cell;
    }

    private PdfPTable barraFirmas(int firmadas, int total) {
        double porcentaje = total > 0 ? (double) firmadas / total : 0;
        PdfPTable barra = new PdfPTable(new float[]{
                (float) Math.max(0.01, porcentaje),
                (float) Math.max(0.01, 1 - porcentaje)});
        barra.setWidthPercentage(100);
        barra.setSpacingBefore(4);
        barra.addCell(celdaBarra(ModaLinkPdfEstilos.AZUL));
        barra.addCell(celdaBarra(new Color(0xE9, 0xEC, 0xF2)));
        return barra;
    }

    private PdfPCell kpi(String etiqueta, String valor) {
        return kpi(etiqueta, valor, null);
    }

    private PdfPCell kpi(String etiqueta, String valor, String detalle) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(GRIS_FONDO);
        cell.setBorderColor(ModaLinkPdfEstilos.BORDE);
        cell.setPadding(8);

        Paragraph label = new Paragraph(
                etiqueta,
                ModaLinkPdfEstilos.fuenteGris(7, Font.NORMAL));
        label.setLeading(9);
        cell.addElement(label);

        Paragraph numero = new Paragraph(
                texto(valor, "—"),
                ModaLinkPdfEstilos.fuente(16, Font.BOLD));
        numero.setLeading(18);
        cell.addElement(numero);

        if (detalle != null) {
            cell.addElement(new Paragraph(
                    detalle,
                    ModaLinkPdfEstilos.fuenteGris(7, Font.NORMAL)));
        }
        return cell;
    }

    private PdfPCell ficha(String etiqueta, String valor) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(GRIS_FONDO);
        cell.setBorderColor(ModaLinkPdfEstilos.BORDE);
        cell.setPadding(7);
        cell.addElement(new Paragraph(
                etiqueta,
                ModaLinkPdfEstilos.fuenteGris(7, Font.NORMAL)));
        cell.addElement(new Paragraph(
                texto(valor, "—"),
                ModaLinkPdfEstilos.fuente(9, Font.BOLD)));
        return cell;
    }

    private PdfPCell meta(String etiqueta, String valor) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(GRIS_FONDO);
        cell.setBorderColor(ModaLinkPdfEstilos.BORDE);
        cell.setPadding(7);
        cell.addElement(new Paragraph(
                etiqueta,
                ModaLinkPdfEstilos.fuenteGris(7, Font.BOLD)));
        cell.addElement(new Paragraph(
                texto(valor, "—"),
                ModaLinkPdfEstilos.fuente(9, Font.NORMAL)));
        return cell;
    }

    private PdfPCell badge(String texto, Color fondo, Color color) {
        PdfPCell cell = new PdfPCell(new Phrase(
                texto,
                fuenteColor(color, 7.5f, Font.BOLD)));
        cell.setBackgroundColor(fondo);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private PdfPCell sinBorde(Paragraph paragraph) {
        PdfPCell cell = new PdfPCell(paragraph);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(0);
        return cell;
    }

    private Font fuenteColor(Color color, float tamanio, int estilo) {
        Font font = ModaLinkPdfEstilos.fuente(tamanio, estilo);
        font.setColor(color);
        return font;
    }

    private Color colorEstado(EstadoAreaInforme estado) {
        return switch (estado) {
            case ROJO -> ROJO;
            case AMARILLO -> AMARILLO;
            case VERDE -> VERDE;
            case SIN_DATOS, NO_APLICA -> GRIS_TEXTO;
        };
    }

    private Color fondoEstado(EstadoAreaInforme estado) {
        return switch (estado) {
            case ROJO -> ROJO_FONDO;
            case AMARILLO -> AMARILLO_FONDO;
            case VERDE -> VERDE_FONDO;
            case SIN_DATOS, NO_APLICA -> GRIS_FONDO;
        };
    }

    private Color colorSeveridad(SeveridadInforme severidad) {
        return switch (severidad) {
            case ALTA -> ROJO;
            case MEDIA -> AMARILLO;
            case BAJA, INFO -> GRIS_TEXTO;
        };
    }

    private Color fondoSeveridad(SeveridadInforme severidad) {
        return switch (severidad) {
            case ALTA -> ROJO_FONDO;
            case MEDIA -> AMARILLO_FONDO;
            case BAJA, INFO -> GRIS_FONDO;
        };
    }

    private EstadoAreaInforme estado(String valor) {
        try {
            return EstadoAreaInforme.valueOf(valor);
        } catch (RuntimeException ex) {
            return EstadoAreaInforme.SIN_DATOS;
        }
    }

    private SeveridadInforme severidad(String valor) {
        try {
            return SeveridadInforme.valueOf(valor);
        } catch (RuntimeException ex) {
            return SeveridadInforme.INFO;
        }
    }

    private String nombreArea(String codigo) {
        return switch (codigo) {
            case "CRONOGRAMA" -> "Cronograma";
            case "EQUIPO" -> "Equipo";
            case "CONTRATOS" -> "Contratos";
            case "BRIEF" -> "Brief";
            default -> texto(codigo, "—");
        };
    }

    private String ubicacion(InformeProyectoResponse datos) {
        var ubicacion = datos.proyecto().ubicacion();
        if (ubicacion == null || ubicacion.ciudad() == null) {
            return "—";
        }
        String ciudad = ubicacion.ciudad().nombre();
        String provincia = ubicacion.ciudad().provincia() != null
                ? ubicacion.ciudad().provincia().nombre()
                : null;
        return provincia == null || provincia.isBlank()
                ? ciudad
                : ciudad + ", " + provincia;
    }

    private String fecha(LocalDate valor) {
        return valor == null ? "—" : valor.format(FECHA);
    }

    private String fecha(LocalDateTime valor) {
        return valor == null ? "—" : valor.format(FECHA);
    }

    private String fechaHora(LocalDateTime valor) {
        return valor == null ? "—" : valor.format(FECHA_HORA);
    }

    private String dias(Integer valor) {
        return valor == null ? "—" : valor + " días";
    }

    private String antiguedad(Long valor) {
        return valor == null ? "—" : valor + " días";
    }

    private String porcentaje(Double valor) {
        return valor == null ? "—" : Math.round(valor * 100) + " %";
    }

    private String texto(String valor, String defecto) {
        return valor == null || valor.isBlank() ? defecto : valor;
    }
}
