package org.mgroko.backend.auditoria.servicio;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.mgroko.backend.admin.servicio.ConfiguracionSistemaService;
import org.mgroko.backend.auditoria.dto.AuditoriaFiltroResuelto;
import org.mgroko.backend.auditoria.exception.AuditoriaFiltroInvalidoException;
import org.mgroko.backend.modelo.TipoEventoAuditoria;
import org.mgroko.backend.repositorio.TipoEventoAuditoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Validación y normalización compartida de los filtros de UC-67.
 *
 * <p>La usan el listado, el resumen y las exportaciones para que los tres
 * apliquen exactamente las mismas reglas.</p>
 */
@Service
public class AuditoriaFiltroService {

    private final ConfiguracionSistemaService configuracionSistemaService;
    private final TipoEventoAuditoriaRepository tipoEventoAuditoriaRepository;

    public AuditoriaFiltroService(
            ConfiguracionSistemaService configuracionSistemaService,
            TipoEventoAuditoriaRepository tipoEventoAuditoriaRepository) {
        this.configuracionSistemaService = configuracionSistemaService;
        this.tipoEventoAuditoriaRepository = tipoEventoAuditoriaRepository;
    }

    @Transactional(readOnly = true)
    public AuditoriaFiltroResuelto resolver(
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            List<String> tiposEvento) {

        validarFechas(fechaDesde, fechaHasta);

        List<TipoEventoAuditoria> catalogo =
                tipoEventoAuditoriaRepository.findByActivoTrueOrderByModuloAscNombreAsc();
        Map<String, TipoEventoAuditoria> catalogoPorCodigo = catalogo.stream()
                .collect(Collectors.toMap(
                        TipoEventoAuditoria::getCodigo,
                        Function.identity(),
                        (existente, duplicado) -> existente,
                        LinkedHashMap::new));

        List<String> tipos = resolverTipos(tiposEvento, catalogoPorCodigo);

        return new AuditoriaFiltroResuelto(
                fechaDesde.atStartOfDay(),
                fechaHasta.atTime(LocalTime.MAX),
                tipos,
                catalogoPorCodigo);
    }

    @Transactional(readOnly = true)
    public void validarPaginacion(int page, int size) {
        int maximo = configuracionSistemaService.obtenerPaginaMaximaAuditoria();

        if (page < 0) {
            throw new AuditoriaFiltroInvalidoException(
                    "PAGINACION_INVALIDA",
                    "La página no puede ser negativa.");
        }
        if (size <= 0 || size > maximo) {
            throw new AuditoriaFiltroInvalidoException(
                    "PAGINACION_INVALIDA",
                    "El tamaño de página debe estar entre 1 y " + maximo + ".");
        }
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

    /**
     * Devuelve {@code null} cuando no corresponde aplicar filtro por tipo.
     * Esto es intencional: sin filtro deben verse también los registros
     * históricos ({@code tipo_evento = NULL}) y los eventos desconocidos.
     */
    private List<String> resolverTipos(
            List<String> tiposEvento,
            Map<String, TipoEventoAuditoria> catalogo) {

        if (tiposEvento == null || tiposEvento.isEmpty()) {
            return null;
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
            return null;
        }

        if (catalogo.isEmpty()) {
            throw new AuditoriaFiltroInvalidoException(
                    "CATALOGO_VACIO",
                    "No hay tipos de evento activos configurados.");
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
}
