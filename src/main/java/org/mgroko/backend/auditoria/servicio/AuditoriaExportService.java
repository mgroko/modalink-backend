package org.mgroko.backend.auditoria.servicio;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.mgroko.backend.admin.servicio.ConfiguracionSistemaService;
import org.mgroko.backend.auditoria.dto.AuditoriaDetalleRequest;
import org.mgroko.backend.auditoria.dto.AuditoriaDetalleResponse;
import org.mgroko.backend.auditoria.dto.AuditoriaEventoResponse;
import org.mgroko.backend.auditoria.dto.AuditoriaFiltroResuelto;
import org.mgroko.backend.auditoria.dto.AuditoriaResumenResponse;
import org.mgroko.backend.auditoria.especificacion.AuditoriaSpecifications;
import org.mgroko.backend.auditoria.exception.ExportacionAuditoriaExcedidaException;
import org.mgroko.backend.auditoria.mapper.AuditoriaMapper;
import org.mgroko.backend.auditoria.pdf.AuditoriaPdfRenderer;
import org.mgroko.backend.auditoria.pdf.AuditoriaPdfRenderer.DatosInforme;
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
    private final AuditoriaPdfRenderer pdfRenderer = new AuditoriaPdfRenderer();

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

        LocalDateTime generadoEn = LocalDateTime.now(reloj);
        DatosInforme informe = new DatosInforme(
                filtro.fechaDesde().toLocalDate(),
                filtro.fechaHasta().toLocalDate(),
                descripcionTipos(filtro),
                nombreAdministrador(idAdministrador),
                generadoEn,
                LocalDateTime.now(reloj),
                resumen,
                datos.eventos(),
                configuracionSistemaService.obtenerMaxCambiosPdfAuditoria());

        pdfRenderer.render(informe, outputStream);

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

    private record DatosExportacion(List<AuditoriaEventoResponse> eventos) {
    }

}
