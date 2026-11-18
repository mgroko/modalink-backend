package org.mgroko.backend.repositorio;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.mgroko.backend.modelo.ColaboracionAutomatica;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Implementación del alta atómica de propuestas de colaboración.
 *
 * <p>Se usa {@code INSERT ... ON CONFLICT DO NOTHING RETURNING} para que dos
 * detecciones concurrentes de la misma pareja de publicaciones no compitan por
 * el índice único parcial. A diferencia de capturar una
 * {@code DataIntegrityViolationException} después de un {@code saveAndFlush},
 * esta sentencia no deja la sesión de Hibernate en estado rollback-only.</p>
 */
@Repository
public class ColaboracionAutomaticaRepositoryImpl
        implements ColaboracionAutomaticaRepositoryCustom {

    private static final String INSERTAR_PENDIENTE_SI_NO_EXISTE = """
            INSERT INTO colaboracion_automatica (
                id_imagen,
                sha256_evidencia,
                phash_version,
                distancia_phash,
                distancia_multicrop,
                orb_matches,
                orb_inliers,
                tipo_coincidencia,
                id_publicacion_a,
                id_publicacion_b,
                id_perfil_a,
                id_perfil_b,
                estado,
                fecha_deteccion
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDIENTE', ?)
            ON CONFLICT (id_publicacion_a, id_publicacion_b)
                WHERE estado = 'PENDIENTE'
            DO NOTHING
            RETURNING id_colaboracion_automatica
            """;

    private final JdbcTemplate jdbcTemplate;

    public ColaboracionAutomaticaRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Long> insertarPendienteSiNoExiste(
            ColaboracionAutomatica colaboracion) {

        List<Long> ids = jdbcTemplate.query(
                INSERTAR_PENDIENTE_SI_NO_EXISTE,
                (rs, rowNum) -> rs.getLong(1),
                colaboracion.getImagen() != null
                        ? colaboracion.getImagen().getIdImagen()
                        : null,
                colaboracion.getSha256Evidencia(),
                colaboracion.getPhashVersion(),
                colaboracion.getDistanciaPhash(),
                colaboracion.getDistanciaMulticrop(),
                colaboracion.getOrbMatches(),
                colaboracion.getOrbInliers(),
                colaboracion.getTipoCoincidencia() != null
                        ? colaboracion.getTipoCoincidencia().name()
                        : null,
                colaboracion.getPublicacionA() != null
                        ? colaboracion.getPublicacionA().getIdPublicacion()
                        : null,
                colaboracion.getPublicacionB() != null
                        ? colaboracion.getPublicacionB().getIdPublicacion()
                        : null,
                colaboracion.getPerfilA().getIdPerfil(),
                colaboracion.getPerfilB().getIdPerfil(),
                Timestamp.valueOf(colaboracion.getFechaDeteccion()));

        return ids.stream().findFirst();
    }
}
