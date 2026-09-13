package org.mgroko.backend.common.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record PaginaResponse<T>(
        List<T> contenido,
        int paginaActual,
        int tamanoPagina,
        long totalElementos,
        int totalPaginas,
        boolean primera,
        boolean ultima
) {
    public static <T> PaginaResponse<T> fromPage(Page<T> page) {
        return new PaginaResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
