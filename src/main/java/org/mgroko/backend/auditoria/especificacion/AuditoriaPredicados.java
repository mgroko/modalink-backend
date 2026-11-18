package org.mgroko.backend.auditoria.especificacion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.auditoria.mapper.AuditoriaMapper;
import org.mgroko.backend.modelo.Auditoria;
import org.mgroko.backend.modelo.DetalleAuditoria;
import org.mgroko.backend.modelo.Usuario;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * Predicados compartidos por el listado, el resumen y las exportaciones de
 * UC-67. Mantenerlos en un único lugar evita que los filtros diverjan.
 */
public final class AuditoriaPredicados {

    private static final char ESCAPE_LIKE = '\\';

    private AuditoriaPredicados() {
    }

    public static List<Predicate> construir(
            Root<Auditoria> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            List<String> tiposEvento,
            String operador,
            String texto) {

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.between(root.get("fechaHora"), fechaDesde, fechaHasta));

        // Sin tipos seleccionados no se filtra: así aparecen los históricos
        // con tipo_evento = NULL y los eventos que no están en el catálogo.
        if (tiposEvento != null && !tiposEvento.isEmpty()) {
            predicates.add(root.get("tipoEvento").in(tiposEvento));
        }

        if (operador != null && !operador.isBlank()) {
            predicates.add(predicadoOperador(root, query, cb, operador));
        }

        if (texto != null && !texto.isBlank()) {
            predicates.add(predicadoTexto(root, query, cb, texto));
        }

        return predicates;
    }

    private static Predicate predicadoOperador(
            Root<Auditoria> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            String operador) {

        Expression<String> patron = patron(cb, operador);

        Predicate snapshot = cb.like(
                normalizar(cb, root.get("operadorNombre")),
                patron,
                ESCAPE_LIKE);

        // Para filas anteriores al snapshot se mantiene el fallback contra el
        // usuario actual. No se backfillean los históricos.
        Subquery<Long> usuarios = query.subquery(Long.class);
        Root<Usuario> usuario = usuarios.from(Usuario.class);
        Expression<String> nombreCompleto = cb.concat(
                cb.concat(
                        cb.<String>coalesce(usuario.get("nombre"), ""),
                        " "),
                cb.<String>coalesce(usuario.get("apellido"), ""));
        Predicate usuarioCoincide = cb.or(
                cb.like(normalizar(cb, usuario.get("nombre")), patron, ESCAPE_LIKE),
                cb.like(normalizar(cb, usuario.get("apellido")), patron, ESCAPE_LIKE),
                cb.like(normalizar(cb, nombreCompleto), patron, ESCAPE_LIKE));
        usuarios.select(cb.literal(1L)).where(
                cb.equal(usuario.get("idUsuario"), root.get("idUsuario")),
                usuarioCoincide);

        Predicate fallback = cb.and(
                cb.isNull(root.get("operadorNombre")),
                cb.exists(usuarios),
                cb.or(
                        cb.isNotNull(root.get("tipoEvento")),
                        cb.isNotNull(root.get("tipoEventoOriginal"))));

        Predicate sistema = cb.and(
                cb.isNull(root.get("idUsuario")),
                cb.like(
                        normalizar(cb, cb.literal(AuditoriaMapper.OPERADOR_SISTEMA)),
                        patron,
                        ESCAPE_LIKE));

        Predicate historico = cb.and(
                cb.isNull(root.get("tipoEvento")),
                cb.isNull(root.get("tipoEventoOriginal")),
                cb.like(
                        normalizar(cb, cb.literal(AuditoriaMapper.OPERADOR_HISTORICO)),
                        patron,
                        ESCAPE_LIKE));

        return cb.or(snapshot, fallback, sistema, historico);
    }

    private static Predicate predicadoTexto(
            Root<Auditoria> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            String texto) {

        Expression<String> patron = patron(cb, texto);

        Subquery<Long> detalles = query.subquery(Long.class);
        Root<DetalleAuditoria> detalle = detalles.from(DetalleAuditoria.class);
        Expression<String> textoDetalle = cb.concat(
                cb.concat(
                        cb.concat(
                                cb.<String>coalesce(detalle.get("campo"), ""),
                                " "),
                        cb.concat(
                                cb.<String>coalesce(detalle.get("valorAnterior"), ""),
                                " ")),
                cb.<String>coalesce(detalle.get("valorNuevo"), ""));
        Predicate detalleCoincide = cb.like(
                normalizar(cb, textoDetalle), patron, ESCAPE_LIKE);
        detalles.select(cb.literal(1L)).where(
                cb.equal(detalle.get("idAuditoria"), root.get("idAuditoria")),
                detalleCoincide);

        Expression<String> textoMetadatos = cb.concat(
                cb.concat(
                        cb.concat(
                                cb.<String>coalesce(root.get("entidadAfectada"), ""),
                                " "),
                        cb.concat(
                                cb.<String>coalesce(root.get("entidadClave"), ""),
                                " ")),
                cb.concat(
                        cb.concat(
                                cb.<String>coalesce(root.get("tipoEvento"), ""),
                                " "),
                        cb.<String>coalesce(root.get("tipoEventoOriginal"), "")));
        Predicate metadatos = cb.like(
                normalizar(cb, textoMetadatos), patron, ESCAPE_LIKE);

        return cb.or(cb.exists(detalles), metadatos);
    }

    private static Expression<String> normalizar(
            CriteriaBuilder cb,
            Expression<String> expresion) {
        return cb.lower(cb.function("f_unaccent", String.class, expresion));
    }

    private static Expression<String> patron(
            CriteriaBuilder cb,
            String texto) {
        String escapado = texto
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        Expression<String> valor = cb.literal("%" + escapado + "%");
        return cb.lower(cb.function("f_unaccent", String.class, valor));
    }
}
