package org.mgroko.backend.admin.dto;

/**
 * Representación plana del género para el panel administrativo.
 *
 * <p>Evita serializar la entidad JPA con su proxy lazy, que fallaba fuera de
 * la transacción con {@code LazyInitializationException}.</p>
 */
public record GeneroResponse(
        Long idGenero,
        String nombre,
        String codigo
) {}
