package org.mgroko.backend.auditoria.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.mgroko.backend.modelo.TipoEventoAuditoria;

/**
 * Filtros de UC-67 ya validados y normalizados.
 *
 * <p>{@code tipos} es {@code null} cuando no se debe aplicar filtro por tipo.
 * Esto permite que el listado incluya registros históricos y eventos que no
 * están en el catálogo.</p>
 */
public record AuditoriaFiltroResuelto(
        LocalDateTime fechaDesde,
        LocalDateTime fechaHasta,
        List<String> tipos,
        String operador,
        String texto,
        Map<String, TipoEventoAuditoria> catalogo
) {
    public boolean tieneFiltroDeTipos() {
        return tipos != null && !tipos.isEmpty();
    }

    public boolean tieneFiltroDeOperador() {
        return operador != null && !operador.isBlank();
    }

    public boolean tieneFiltroDeTexto() {
        return texto != null && !texto.isBlank();
    }

    public List<String> tiposAplicados() {
        return tipos == null ? List.of() : tipos;
    }
}
