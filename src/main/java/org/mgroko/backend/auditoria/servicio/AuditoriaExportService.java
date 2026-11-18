package org.mgroko.backend.auditoria.servicio;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.mgroko.backend.admin.servicio.ConfiguracionSistemaService;
import org.mgroko.backend.auditoria.dto.AuditoriaDetalleRequest;
import org.mgroko.backend.auditoria.dto.AuditoriaDetalleResponse;
import org.mgroko.backend.auditoria.dto.AuditoriaEventoResponse;
import org.mgroko.backend.auditoria.dto.AuditoriaFiltroResuelto;
import org.mgroko.backend.auditoria.dto.AuditoriaResumenResponse;
import org.mgroko.backend.auditoria.dto.AuditoriaResumenTipoResponse;
import org.mgroko.backend.auditoria.especificacion.AuditoriaSpecifications;
import org.mgroko.backend.auditoria.exception.ErrorGenerandoPdfException;
import org.mgroko.backend.auditoria.exception.ExportacionAuditoriaExcedidaException;
import org.mgroko.backend.auditoria.mapper.AuditoriaMapper;
import org.mgroko.backend.modelo.Auditoria;
import org.mgroko.backend.modelo.DetalleAuditoria;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.repositorio.AuditoriaRepository;
import org.mgroko.backend.repositorio.DetalleAuditoriaRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfWriter;

/**
 * UC-67: exportación del resultado filtrado a CSV y PDF.
 *
 * <p>Los métodos de escritura son transaccionales y escriben directamente al
 * {@link OutputStream}. El controller valida el tope antes de abrir la
 * descarga, de modo que los errores de filtro se devuelvan como JSON.</p>
 */
@Service
public class AuditoriaExportService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaExportService.class);

    private static final int LOTE_IDS = 1_000;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final String[] CSV_HEADER = {
            "id_auditoria",
            "fecha_hora",
            "tipo_evento",
            "tipo_evento_original",
            "tipo_evento_nombre",
            "entidad_afectada",
            "entidad_clave",
            "id_afectado",
            "operador",
            "legacy",
            "usuario_bd",
            "ip_usuario",
            "cambios"
    };

    private final AuditoriaRepository auditoriaRepository;
    private final DetalleAuditoriaRepository detalleAuditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaFiltroService auditoriaFiltroService;
    private final AuditoriaResumenService auditoriaResumenService;
    private final AuditoriaMapper auditoriaMapper;
    private final ConfiguracionSistemaService configuracionSistemaService;
    private final AuditoriaEventoService auditoriaEventoService;
    private final Clock reloj;

    public AuditoriaExportService(
            AuditoriaRepository auditoriaRepository,
            DetalleAuditoriaRepository detalleAuditoriaRepository,
            UsuarioRepository usuarioRepository,
            AuditoriaFiltroService auditoriaFiltroService,
            AuditoriaResumenService auditoriaResumenService,
            AuditoriaMapper auditoriaMapper,
            ConfiguracionSistemaService configuracionSistemaService,
            AuditoriaEventoService auditoriaEventoService,
            Clock reloj) {
        this.auditoriaRepository = auditoriaRepository;
        this.detalleAuditoriaRepository = detalleAuditoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaFiltroService = auditoriaFiltroService;
        this.auditoriaResumenService = auditoriaResumenService;
        this.auditoriaMapper = auditoriaMapper;
        this.configuracionSistemaService = configuracionSistemaService;
        this.auditoriaEventoService = auditoriaEventoService;
        this.reloj = reloj;
    }

    /**
     * Valida el filtro y el tope antes de que el controller abra la descarga.
     */
    @Transactional(readOnly = true)
    public AuditoriaFiltroResuelto validarExportacion(
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            List<String> tiposEvento,
            String operador,
            String texto) {

        AuditoriaFiltroResuelto filtro =
                auditoriaFiltroService.resolver(
                        fechaDesde, fechaHasta, tiposEvento, operador, texto);

        long total = auditoriaRepository.count(AuditoriaSpecifications.conFiltros(
                filtro.fechaDesde(), filtro.fechaHasta(), filtro.tipos(),
                filtro.operador(), filtro.texto()));

        int maximo = configuracionSistemaService.obtenerMaximoExportacionAuditoria();
        if (total > maximo) {
            throw new ExportacionAuditoriaExcedidaException(total, maximo);
        }
        return filtro;
    }

    @Transactional
    public void exportarCsv(AuditoriaFiltroResuelto filtro, OutputStream outputStream) {
        DatosExportacion datos = cargarDatos(filtro);

        try {
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
            writer.write('\uFEFF');
            writer.write(String.join(";", CSV_HEADER));
            writer.newLine();

            for (AuditoriaEventoResponse evento : datos.eventos()) {
                writer.write(String.join(";",
                        csv(evento.idAuditoria()),
                        csv(evento.fechaHora()),
                        csv(evento.tipoEvento()),
                        csv(evento.tipoEventoOriginal()),
                        csv(evento.tipoEventoNombre()),
                        csv(evento.entidadAfectada()),
                        csv(evento.entidadClave()),
                        csv(evento.idAfectado()),
                        csv(evento.operador()),
                        csv(evento.legacy()),
                        csv(evento.usuarioBd()),
                        csv(evento.ipUsuario()),
                        csv(cambios(evento.detalle()))));
                writer.newLine();
            }
            writer.flush();
        } catch (IOException ex) {
            log.error("No se pudo generar el CSV de auditoría para el rango {} - {}",
                    filtro.fechaDesde(), filtro.fechaHasta(), ex);
            throw new IllegalStateException("No se pudo generar el CSV de auditoría.", ex);
        }

        registrarExportacion("CSV", filtro, datos.eventos().size());
    }

    @Transactional
    public void exportarPdf(
            AuditoriaFiltroResuelto filtro,
            Long idAdministrador,
            OutputStream outputStream) {

        DatosExportacion datos = cargarDatos(filtro);
        AuditoriaResumenResponse resumen = auditoriaResumenService.obtenerResumen(
                filtro.fechaDesde().toLocalDate(),
                filtro.fechaHasta().toLocalDate(),
                filtro.tipos(),
                filtro.operador(),
                filtro.texto());

        Document document = new Document(PageSize.A4.rotate(), 24, 24, 32, 32);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            writer.setPageEvent(new PieDePaginaEvent());

            document.open();

            BaseFont base = crearBaseFont();
            Font titulo = new Font(base, 16, Font.BOLD);
            Font subtitulo = new Font(base, 10, Font.BOLD);
            Font normal = new Font(base, 9, Font.NORMAL);
            Font celda = new Font(base, 8, Font.NORMAL);
            Font celdaBold = new Font(base, 8, Font.BOLD);

            document.add(new Paragraph("Informe de auditoría", titulo));
            document.add(new Paragraph("Sistema: ModaLink", normal));
            document.add(new Paragraph("Rango: " + filtro.fechaDesde().toLocalDate()
                    + " a " + filtro.fechaHasta().toLocalDate(), normal));
            document.add(new Paragraph("Tipos de evento: " + descripcionTipos(filtro), normal));
            document.add(new Paragraph("Generado: "
                    + LocalDateTime.now(reloj).format(FECHA), normal));
            document.add(new Paragraph("Generado por: "
                    + nombreAdministrador(idAdministrador), normal));
            document.add(new Paragraph("Total de registros: " + resumen.totalRegistros(), normal));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Resumen por tipo de evento", subtitulo));
            document.add(tablaResumenTipos(resumen.porTipoEvento(), celda, celdaBold));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Resumen por entidad afectada", subtitulo));
            document.add(tablaResumenEntidades(resumen, celda, celdaBold));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Eventos", subtitulo));
            document.add(tablaEventos(datos.eventos(), celda, celdaBold));
        } catch (DocumentException ex) {
            log.error("No se pudo generar el PDF de auditoría para el rango {} - {}",
                    filtro.fechaDesde(), filtro.fechaHasta(), ex);
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

        registrarExportacion("PDF", filtro, datos.eventos().size());
    }

    private void registrarExportacion(
            String formato,
            AuditoriaFiltroResuelto filtro,
            int registros) {

        auditoriaEventoService.registrar(
                "auditoria",
                0L,
                "AUDITORIA_EXPORTADA",
                "UPDATE",
                List.of(
                        new AuditoriaDetalleRequest("formato", null, formato),
                        new AuditoriaDetalleRequest("fechaDesde", null,
                                filtro.fechaDesde().toLocalDate().toString()),
                        new AuditoriaDetalleRequest("fechaHasta", null,
                                filtro.fechaHasta().toLocalDate().toString()),
                        new AuditoriaDetalleRequest("tipos", null,
                                filtro.tieneFiltroDeTipos()
                                        ? String.join(",", filtro.tipos())
                                        : "TODOS"),
                        new AuditoriaDetalleRequest("operador", null,
                                filtro.tieneFiltroDeOperador()
                                        ? filtro.operador()
                                        : "TODOS"),
                        new AuditoriaDetalleRequest("texto", null,
                                filtro.tieneFiltroDeTexto()
                                        ? filtro.texto()
                                        : "TODOS"),
                        new AuditoriaDetalleRequest("registros", null,
                                String.valueOf(registros))));
    }

    private DatosExportacion cargarDatos(AuditoriaFiltroResuelto filtro) {
        List<Auditoria> registros = auditoriaRepository.findAll(
                AuditoriaSpecifications.conFiltros(
                        filtro.fechaDesde(), filtro.fechaHasta(), filtro.tipos(),
                        filtro.operador(), filtro.texto()),
                Sort.by(
                        Sort.Order.desc("fechaHora"),
                        Sort.Order.desc("idAuditoria")));

        int maximo = configuracionSistemaService.obtenerMaximoExportacionAuditoria();
        if (registros.size() > maximo) {
            throw new ExportacionAuditoriaExcedidaException(registros.size(), maximo);
        }

        List<Long> idsAuditoria = registros.stream()
                .map(Auditoria::getIdAuditoria)
                .toList();

        Map<Long, List<DetalleAuditoria>> detalles = cargarDetalles(idsAuditoria);

        Set<Long> idsUsuarios = registros.stream()
                .map(Auditoria::getIdUsuario)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<Long, Usuario> usuarios = cargarUsuarios(idsUsuarios);

        List<AuditoriaEventoResponse> eventos = registros.stream()
                .map(auditoria -> auditoriaMapper.toResponse(
                        auditoria, filtro.catalogo(), detalles, usuarios))
                .toList();

        return new DatosExportacion(eventos);
    }

    private Map<Long, List<DetalleAuditoria>> cargarDetalles(List<Long> idsAuditoria) {
        Map<Long, List<DetalleAuditoria>> resultado = new HashMap<>();
        for (int inicio = 0; inicio < idsAuditoria.size(); inicio += LOTE_IDS) {
            int fin = Math.min(inicio + LOTE_IDS, idsAuditoria.size());
            List<Long> lote = idsAuditoria.subList(inicio, fin);
            detalleAuditoriaRepository
                    .findByIdAuditoriaInOrderByIdDetalleAuditoriaAsc(lote)
                    .forEach(detalle -> resultado
                            .computeIfAbsent(detalle.getIdAuditoria(), clave -> new ArrayList<>())
                            .add(detalle));
        }
        return resultado;
    }

    private Map<Long, Usuario> cargarUsuarios(Set<Long> idsUsuarios) {
        List<Long> ids = new ArrayList<>(idsUsuarios);
        Map<Long, Usuario> resultado = new HashMap<>();
        for (int inicio = 0; inicio < ids.size(); inicio += LOTE_IDS) {
            int fin = Math.min(inicio + LOTE_IDS, ids.size());
            usuarioRepository.findAllById(ids.subList(inicio, fin))
                    .forEach(usuario -> resultado.put(usuario.getIdUsuario(), usuario));
        }
        return resultado;
    }

    private PdfPTable tablaResumenTipos(
            List<AuditoriaResumenTipoResponse> tipos,
            Font fuente,
            Font fuenteNegrita) {
        PdfPTable tabla = new PdfPTable(new float[]{2.2f, 4.5f, 1.0f});
        tabla.setWidthPercentage(100);
        encabezado(tabla, fuenteNegrita, "Código", "Nombre", "Total");

        if (tipos.isEmpty()) {
            tabla.addCell(celda("Sin datos", fuente, 3));
            return tabla;
        }

        for (AuditoriaResumenTipoResponse tipo : tipos) {
            tabla.addCell(celda(tipo.codigo(), fuente, 1));
            tabla.addCell(celda(tipo.nombre(), fuente, 1));
            tabla.addCell(celda(String.valueOf(tipo.total()), fuente, 1));
        }
        return tabla;
    }

    private PdfPTable tablaResumenEntidades(
            AuditoriaResumenResponse resumen,
            Font fuente,
            Font fuenteNegrita) {
        PdfPTable tabla = new PdfPTable(new float[]{4.0f, 1.0f});
        tabla.setWidthPercentage(60);
        encabezado(tabla, fuenteNegrita, "Entidad", "Total");

        if (resumen.porEntidad().isEmpty()) {
            tabla.addCell(celda("Sin datos", fuente, 2));
            return tabla;
        }

        resumen.porEntidad().forEach(entidad -> {
            tabla.addCell(celda(entidad.entidad(), fuente, 1));
            tabla.addCell(celda(String.valueOf(entidad.total()), fuente, 1));
        });
        return tabla;
    }

    private PdfPTable tablaEventos(
            List<AuditoriaEventoResponse> eventos,
            Font fuente,
            Font fuenteNegrita) {
        PdfPTable tabla = new PdfPTable(new float[]{
                1.35f, 1.85f, 1.85f, 1.75f, 1.00f, 1.35f, 1.95f, 1.95f});
        tabla.setWidthPercentage(100);
        tabla.setHeaderRows(1);
        encabezado(tabla, fuenteNegrita,
                "Fecha", "Evento", "Entidad / ID", "Operador", "IP",
                "Campo", "Valor anterior", "Valor actual");

        if (eventos.isEmpty()) {
            tabla.addCell(celda("Sin registros para los filtros seleccionados.", fuente, 8));
            return tabla;
        }

        int maxCambios = configuracionSistemaService.obtenerMaxCambiosPdfAuditoria();
        for (AuditoriaEventoResponse evento : eventos) {
            List<AuditoriaDetalleResponse> detalles = evento.detalle();

            if (detalles.isEmpty()) {
                agregarFilaEvento(tabla, evento, "", "", "", fuente);
                continue;
            }

            int mostrados = Math.min(detalles.size(), maxCambios);
            for (int i = 0; i < mostrados; i++) {
                AuditoriaDetalleResponse detalle = detalles.get(i);
                agregarFilaEvento(tabla, evento,
                        valorVisible(detalle.campo()),
                        valorVisible(detalle.valorAnterior()),
                        valorVisible(detalle.valorNuevo()),
                        fuente);
            }

            if (detalles.size() > mostrados) {
                agregarFilaEvento(tabla, evento,
                        "+" + (detalles.size() - mostrados) + " más", "", "", fuente);
            }
        }
        return tabla;
    }

    private void agregarFilaEvento(
            PdfPTable tabla,
            AuditoriaEventoResponse evento,
            String campo,
            String valorAnterior,
            String valorNuevo,
            Font fuente) {

        tabla.addCell(celda(evento.fechaHora().format(FECHA), fuente, 1));
        tabla.addCell(celda(nombreEvento(evento), fuente, 1));
        tabla.addCell(celda(entidad(evento), fuente, 1));
        tabla.addCell(celda(evento.operador(), fuente, 1));
        tabla.addCell(celda(evento.ipUsuario(), fuente, 1));
        tabla.addCell(celda(campo, fuente, 1));
        tabla.addCell(celda(valorAnterior, fuente, 1));
        tabla.addCell(celda(valorNuevo, fuente, 1));
    }

    private String valorVisible(String valor) {
        return valor == null || valor.isBlank() ? "—" : valor;
    }

    private void encabezado(PdfPTable tabla, Font fuenteNegrita, String... titulos) {
        for (String titulo : titulos) {
            PdfPCell celda = new PdfPCell(new Phrase(titulo, fuenteNegrita));
            celda.setGrayFill(0.90f);
            celda.setPadding(4);
            tabla.addCell(celda);
        }
    }

    private PdfPCell celda(String valor, Font fuente, int colspan) {
        PdfPCell celda = new PdfPCell(new Phrase(valor == null ? "" : valor, fuente));
        celda.setColspan(colspan);
        celda.setPadding(4);
        return celda;
    }

    private String nombreEvento(AuditoriaEventoResponse evento) {
        return evento.tipoEventoNombre() != null
                ? evento.tipoEventoNombre()
                : evento.tipoEvento() != null ? evento.tipoEvento() : "Sin tipo (histórico)";
    }

    private String entidad(AuditoriaEventoResponse evento) {
        if (evento.entidadClave() != null && !evento.entidadClave().isBlank()) {
            return evento.entidadAfectada() + " / " + evento.entidadClave();
        }
        return evento.entidadAfectada() + " / " + evento.idAfectado();
    }

    private String descripcionTipos(AuditoriaFiltroResuelto filtro) {
        if (!filtro.tieneFiltroDeTipos()) {
            return "Todos";
        }
        return String.join(", ", filtro.tipos());
    }

    private String nombreAdministrador(Long idAdministrador) {
        if (idAdministrador == null) {
            return "No informado";
        }
        return usuarioRepository.findById(idAdministrador)
                .map(this::nombreCompleto)
                .orElse("Usuario #" + idAdministrador);
    }

    private String nombreCompleto(Usuario usuario) {
        String nombre = usuario.getNombre() != null ? usuario.getNombre() : "";
        String apellido = usuario.getApellido() != null ? usuario.getApellido() : "";
        String completo = (nombre + " " + apellido).trim();
        return completo.isEmpty() ? "Usuario #" + usuario.getIdUsuario() : completo;
    }

    private String cambios(List<AuditoriaDetalleResponse> detalle) {
        return detalle.stream()
                .map(item -> valor(item.campo()) + ": "
                        + valor(item.valorAnterior()) + " -> " + valor(item.valorNuevo()))
                .collect(Collectors.joining(" | "));
    }

    private String valor(String valor) {
        return valor == null ? "" : valor;
    }

    private String csv(Object valor) {
        if (valor == null) {
            return "";
        }
        String texto = String.valueOf(valor);
        if (texto.isEmpty()) {
            return "";
        }
        if (texto.charAt(0) == '=' || texto.charAt(0) == '+'
                || texto.charAt(0) == '-' || texto.charAt(0) == '@'
                || texto.charAt(0) == '\t' || texto.charAt(0) == '\r') {
            texto = "'" + texto;
        }
        if (texto.contains(";") || texto.contains("\"")
                || texto.contains("\n") || texto.contains("\r")) {
            texto = "\"" + texto.replace("\"", "\"\"") + "\"";
        }
        return texto;
    }

    private static BaseFont crearBaseFont() {
        try {
            return BaseFont.createFont(
                    BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED);
        } catch (DocumentException | IOException ex) {
            throw new ErrorGenerandoPdfException("No se pudo cargar la fuente del PDF.", ex);
        }
    }

    private record DatosExportacion(List<AuditoriaEventoResponse> eventos) {
    }

    private static class PieDePaginaEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try {
                Phrase pie = new Phrase("Página " + writer.getPageNumber(),
                        new Font(crearBaseFont(), 8, Font.NORMAL));
                PdfContentByte canvas = writer.getDirectContent();
                ColumnText.showTextAligned(
                        canvas,
                        Element.ALIGN_CENTER,
                        pie,
                        (document.left() + document.right()) / 2,
                        document.bottom() - 12,
                        0);
            } catch (RuntimeException ignored) {
                // El pie es informativo: no debe interrumpir la exportación.
            }
        }
    }
}
