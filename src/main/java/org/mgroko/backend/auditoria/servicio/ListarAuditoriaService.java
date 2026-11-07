package org.mgroko.backend.auditoria.servicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.mgroko.backend.admin.servicio.ConfiguracionSistemaService;
import org.mgroko.backend.auditoria.dto.AuditoriaDetalleResponse;
import org.mgroko.backend.auditoria.dto.AuditoriaEventoResponse;
import org.mgroko.backend.auditoria.dto.TipoEventoAuditoriaResponse;
import org.mgroko.backend.auditoria.especificacion.AuditoriaSpecifications;
import org.mgroko.backend.auditoria.exception.AuditoriaFiltroInvalidoException;
import org.mgroko.backend.common.dto.PaginaResponse;
import org.mgroko.backend.modelo.Auditoria;
import org.mgroko.backend.modelo.DetalleAuditoria;
import org.mgroko.backend.modelo.TipoEventoAuditoria;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.repositorio.AuditoriaRepository;
import org.mgroko.backend.repositorio.DetalleAuditoriaRepository;
import org.mgroko.backend.repositorio.TipoEventoAuditoriaRepository;
import org.mgroko.backend.repositorio.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-67: consulta paginada del registro de auditoría.
 */
@Service
public class ListarAuditoriaService {

    private static final int TAMANO_MAXIMO = 100;

    private final AuditoriaRepository auditoriaRepository;
    private final DetalleAuditoriaRepository detalleAuditoriaRepository;
    private final TipoEventoAuditoriaRepository tipoEventoAuditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ConfiguracionSistemaService configuracionSistemaService;

    public ListarAuditoriaService(
            AuditoriaRepository auditoriaRepository,
            DetalleAuditoriaRepository detalleAuditoriaRepository,
            TipoEventoAuditoriaRepository tipoEventoAuditoriaRepository,
            UsuarioRepository usuarioRepository,
            ConfiguracionSistemaService configuracionSistemaService) {
        this.auditoriaRepository = auditoriaRepository;
        this.detalleAuditoriaRepository = detalleAuditoriaRepository;
        this.tipoEventoAuditoriaRepository = tipoEventoAuditoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.configuracionSistemaService = configuracionSistemaService;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<AuditoriaEventoResponse> listar(
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            List<String> tiposEvento,
            int page,
            int size) {

        validarFechas(fechaDesde, fechaHasta);
        validarPaginacion(page, size);

        List<TipoEventoAuditoria> catalogo =
                tipoEventoAuditoriaRepository.findByActivoTrueOrderByModuloAscNombreAsc();
        Map<String, TipoEventoAuditoria> catalogoPorCodigo = catalogo.stream()
                .collect(Collectors.toMap(TipoEventoAuditoria::getCodigo, Function.identity()));

        List<String> tiposFiltro = resolverTipos(tiposEvento, catalogoPorCodigo);

        LocalDateTime desde = fechaDesde.atStartOfDay();
        LocalDateTime hasta = fechaHasta.atTime(LocalTime.MAX);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("fechaHora"),
                        Sort.Order.desc("idAuditoria")));

        Page<Auditoria> pagina = auditoriaRepository.findAll(
                AuditoriaSpecifications.conFiltros(desde, hasta, tiposFiltro),
                pageable);

        if (pagina.isEmpty()) {
            return PaginaResponse.fromPage(pagina.map(auditoria -> toResponse(
                    auditoria,
                    catalogoPorCodigo,
                    Collections.emptyMap(),
                    Collections.emptyMap())));
        }

        List<Long> idsAuditoria = pagina.getContent().stream()
                .map(Auditoria::getIdAuditoria)
                .toList();

        Map<Long, List<DetalleAuditoria>> detallesPorAuditoria =
                detalleAuditoriaRepository
                        .findByIdAuditoriaInOrderByIdDetalleAuditoriaAsc(idsAuditoria)
                        .stream()
                        .collect(Collectors.groupingBy(DetalleAuditoria::getIdAuditoria));

        Set<Long> idsUsuarios = pagina.getContent().stream()
                .map(Auditoria::getIdUsuario)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, Usuario> usuarios = idsUsuarios.isEmpty()
                ? Collections.emptyMap()
                : usuarioRepository.findAllById(idsUsuarios).stream()
                        .collect(Collectors.toMap(Usuario::getIdUsuario, Function.identity()));

        return PaginaResponse.fromPage(pagina.map(auditoria -> toResponse(
                auditoria,
                catalogoPorCodigo,
                detallesPorAuditoria,
                usuarios)));
    }

    @Transactional(readOnly = true)
    public List<TipoEventoAuditoriaResponse> listarTipos() {
        return tipoEventoAuditoriaRepository.findByActivoTrueOrderByModuloAscNombreAsc()
                .stream()
                .map(tipo -> new TipoEventoAuditoriaResponse(
                        tipo.getCodigo(),
                        tipo.getNombre(),
                        tipo.getDescripcion(),
                        tipo.getModulo()))
                .toList();
    }

    private void validarFechas(LocalDate fechaDesde, LocalDate fechaHasta) {
        if (fechaDesde == null || fechaHasta == null) {
            throw new AuditoriaFiltroInvalidoException(
                    "FECHAS_OBLIGATORIAS",
                    "Las fechas desde y hasta son obligatorias.");
        }
        if (fechaDesde.isAfter(fechaHasta)) {
            throw new AuditoriaFiltroInvalidoException(
                    "RANGO_FECHAS_INVALIDO",
                    "La fecha desde no puede ser posterior a la fecha hasta.");
        }

        long dias = ChronoUnit.DAYS.between(fechaDesde, fechaHasta) + 1;
        int maximo = configuracionSistemaService.obtenerRangoMaximoAuditoriaDias();
        if (dias > maximo) {
            throw new AuditoriaFiltroInvalidoException(
                    "RANGO_FECHAS_EXCEDIDO",
                    "El rango máximo permitido es de " + maximo + " días.");
        }
    }

    private void validarPaginacion(int page, int size) {
        if (page < 0) {
            throw new AuditoriaFiltroInvalidoException(
                    "PAGINACION_INVALIDA",
                    "La página no puede ser negativa.");
        }
        if (size <= 0 || size > TAMANO_MAXIMO) {
            throw new AuditoriaFiltroInvalidoException(
                    "PAGINACION_INVALIDA",
                    "El tamaño de página debe estar entre 1 y " + TAMANO_MAXIMO + ".");
        }
    }

    private List<String> resolverTipos(
            List<String> tiposEvento,
            Map<String, TipoEventoAuditoria> catalogo) {

        if (catalogo.isEmpty()) {
            return Collections.emptyList();
        }
        if (tiposEvento == null || tiposEvento.isEmpty()) {
            return new ArrayList<>(catalogo.keySet());
        }

        LinkedHashSet<String> normalizados = new LinkedHashSet<>();
        for (String valor : tiposEvento) {
            if (valor == null || valor.isBlank()) {
                continue;
            }
            for (String parte : valor.split(",")) {
                String codigo = parte.trim().toUpperCase(Locale.ROOT);
                if (!codigo.isEmpty()) {
                    normalizados.add(codigo);
                }
            }
        }

        if (normalizados.isEmpty()) {
            return new ArrayList<>(catalogo.keySet());
        }

        for (String codigo : normalizados) {
            if (!catalogo.containsKey(codigo)) {
                throw new AuditoriaFiltroInvalidoException(
                        "TIPO_EVENTO_INVALIDO",
                        "Tipo de evento inválido: " + codigo);
            }
        }

        return new ArrayList<>(normalizados);
    }

    private AuditoriaEventoResponse toResponse(
            Auditoria auditoria,
            Map<String, TipoEventoAuditoria> catalogo,
            Map<Long, List<DetalleAuditoria>> detallesPorAuditoria,
            Map<Long, Usuario> usuarios) {

        TipoEventoAuditoria tipo = auditoria.getTipoEvento() == null
                ? null
                : catalogo.get(auditoria.getTipoEvento());

        Usuario usuario = auditoria.getIdUsuario() == null
                ? null
                : usuarios.get(auditoria.getIdUsuario());

        List<AuditoriaDetalleResponse> detalle = detallesPorAuditoria
                .getOrDefault(auditoria.getIdAuditoria(), Collections.emptyList())
                .stream()
                .map(item -> new AuditoriaDetalleResponse(
                        item.getCampo(),
                        item.getValorAnterior(),
                        item.getValorNuevo()))
                .toList();

        return new AuditoriaEventoResponse(
                auditoria.getIdAuditoria(),
                auditoria.getFechaHora(),
                auditoria.getTipoEvento(),
                tipo != null ? tipo.getNombre() : null,
                auditoria.getEntidadAfectada(),
                auditoria.getEntidadClave(),
                auditoria.getIdAfectado(),
                auditoria.getIdUsuario(),
                usuario != null ? nombreCompleto(usuario) : null,
                auditoria.getUsuarioBd(),
                auditoria.getIpUsuario(),
                detalle);
    }

    private String nombreCompleto(Usuario usuario) {
        String nombre = usuario.getNombre() != null ? usuario.getNombre() : "";
        String apellido = usuario.getApellido() != null ? usuario.getApellido() : "";
        String completo = (nombre + " " + apellido).trim();
        return completo.isEmpty() ? null : completo;
    }
}
