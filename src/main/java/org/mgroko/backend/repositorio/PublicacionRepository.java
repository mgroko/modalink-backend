package org.mgroko.backend.repositorio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.Publicacion;
import org.mgroko.backend.modelo.enums.EstadoPublicacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface PublicacionRepository extends JpaRepository<Publicacion, Long>,
        JpaSpecificationExecutor<Publicacion> {

    /**
     * Publicaciones de un perfil en el estado indicado, más recientes primero.
     * Se inicializan las asociaciones necesarias para armar la tarjeta sin
     * provocar N+1; las imágenes se cargan en lote por {@code @BatchSize}.
     */
    @EntityGraph(attributePaths = {
            "perfil",
            "perfil.usuario",
            "perfil.profesion",
            "perfil.imagen",
            "proyecto",
            "ubicacion",
            "ubicacion.ciudad",
            "ubicacion.ciudad.provincia",
            "ubicacion.ciudad.provincia.pais"
    })
    @Query("""
            SELECT p
            FROM Publicacion p
            WHERE p.estado = :estado
              AND (
                p.perfil.idPerfil = :idPerfil
                OR (
                  EXISTS (SELECT c FROM p.colaboradores c WHERE c.idPerfil = :idPerfil)
                  AND NOT EXISTS (
                    SELECT 1
                    FROM ColaboradorPublicacionOrigen o
                    WHERE o.idPublicacion = p.idPublicacion
                      AND o.idPerfil = :idPerfil
                      AND o.origen = org.mgroko.backend.modelo.enums.OrigenColaboracion.AUTOMATICA
                  )
                  AND p.perfil.estado = org.mgroko.backend.modelo.enums.EstadoPerfil.ACTIVO
                  AND p.perfil.usuario.estado = org.mgroko.backend.modelo.enums.EstadoUsuario.ACTIVO
                )
              )
            ORDER BY p.fechaPublicacion DESC, p.idPublicacion DESC
            """)
    Page<Publicacion> findByPerfilIdPerfilAndEstadoOrderByFechaPublicacionDesc(
            @Param("idPerfil") Long idPerfil,
            @Param("estado") EstadoPublicacion estado,
            Pageable pageable);

    /**
     * Proyectos distintos asociados a publicaciones activas del perfil.
     * Se usan para poblar el selector de proyecto de UC-22.
     */
    @Query("""
            SELECT DISTINCT p.proyecto
            FROM Publicacion p
            WHERE p.estado = :estado
              AND p.proyecto IS NOT NULL
              AND (
                p.perfil.idPerfil = :idPerfil
                OR (
                  EXISTS (SELECT c FROM p.colaboradores c WHERE c.idPerfil = :idPerfil)
                  AND NOT EXISTS (
                    SELECT 1
                    FROM ColaboradorPublicacionOrigen o
                    WHERE o.idPublicacion = p.idPublicacion
                      AND o.idPerfil = :idPerfil
                      AND o.origen = org.mgroko.backend.modelo.enums.OrigenColaboracion.AUTOMATICA
                  )
                  AND p.perfil.estado = org.mgroko.backend.modelo.enums.EstadoPerfil.ACTIVO
                  AND p.perfil.usuario.estado = org.mgroko.backend.modelo.enums.EstadoUsuario.ACTIVO
                )
              )
            ORDER BY p.proyecto.nombre
            """)
    List<Proyecto> findProyectosConPublicaciones(
            @Param("idPerfil") Long idPerfil,
            @Param("estado") EstadoPublicacion estado);

    /**
     * UC-21: publicación propia con las asociaciones que necesita el
     * formulario de edición. Las colecciones ({@code imagenes} y
     * {@code colaboradores}) se cargan en lote para no armar un producto
     * cartesiano ni romper la paginación de los listados.
     */
    @EntityGraph(attributePaths = {
            "perfil",
            "perfil.profesion",
            "perfil.imagen",
            "proyecto",
            "ubicacion",
            "ubicacion.ciudad",
            "ubicacion.ciudad.provincia",
            "ubicacion.ciudad.provincia.pais"
    })
    Optional<Publicacion> findByIdPublicacionAndPerfilIdPerfil(
            Long idPublicacion, Long idPerfil);

    /**
     * Detalle de una publicación visible para el perfil activo, sin acotar al
     * dueño. Carga las asociaciones necesarias para la tarjeta de detalle.
     */
    @EntityGraph(attributePaths = {
            "perfil",
            "perfil.usuario",
            "perfil.profesion",
            "perfil.imagen",
            "proyecto",
            "ubicacion",
            "ubicacion.ciudad",
            "ubicacion.ciudad.provincia",
            "ubicacion.ciudad.provincia.pais"
    })
    Optional<Publicacion> findByIdPublicacion(Long idPublicacion);

    /**
     * UC-69: carga en lote las publicaciones reportadas junto con las
     * asociaciones necesarias para armar la bandeja administrativa.
     */
    @EntityGraph(attributePaths = {
            "perfil",
            "perfil.usuario",
            "perfil.profesion",
            "proyecto",
            "ubicacion",
            "ubicacion.ciudad",
            "ubicacion.ciudad.provincia",
            "ubicacion.ciudad.provincia.pais"
    })
    @Query("SELECT p FROM Publicacion p WHERE p.idPublicacion IN :ids")
    List<Publicacion> findDetallePorIds(@Param("ids") Collection<Long> ids);

    /**
     * UC-61: indica si un perfil ya es colaborador de la publicación.
     */
    @Query("""
            SELECT COUNT(c) > 0
            FROM Publicacion p
            JOIN p.colaboradores c
            WHERE p.idPublicacion = :idPublicacion
              AND c.idPerfil = :idPerfil
            """)
    boolean existsColaborador(
            @Param("idPublicacion") Long idPublicacion,
            @Param("idPerfil") Long idPerfil);

    /**
     * UC-21: actualiza el orden de una imagen existente sin pasar por la
     * colección {@code @OrderColumn}. Hibernate reordena actualizando
     * {@code id_imagen} por posición, lo que viola la PK si se intercambian
     * dos imágenes. Actualizar solo {@code orden} evita el conflicto.
     */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            UPDATE imagen_publicacion
            SET orden = :orden
            WHERE id_publicacion = :idPublicacion
              AND id_imagen = :idImagen
            """, nativeQuery = true)
    int actualizarOrdenImagenPublicacion(
            @Param("idPublicacion") Long idPublicacion,
            @Param("idImagen") Long idImagen,
            @Param("orden") int orden);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            DELETE FROM imagen_publicacion
            WHERE id_publicacion = :idPublicacion
              AND id_imagen = :idImagen
            """, nativeQuery = true)
    int eliminarImagenPublicacion(
            @Param("idPublicacion") Long idPublicacion,
            @Param("idImagen") Long idImagen);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO imagen_publicacion (id_publicacion, id_imagen, orden)
            VALUES (:idPublicacion, :idImagen, :orden)
            """, nativeQuery = true)
    int insertarImagenPublicacion(
            @Param("idPublicacion") Long idPublicacion,
            @Param("idImagen") Long idImagen,
            @Param("orden") int orden);

    /**
     * UC-20: bloquea la publicación para que la baja no compita con la
     * aceptación de una solicitud de colaboración ni con otra baja.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM Publicacion p
            WHERE p.idPublicacion = :idPublicacion
            """)
    Optional<Publicacion> findByIdPublicacionForUpdate(
            @Param("idPublicacion") Long idPublicacion);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            DELETE FROM imagen_publicacion
            WHERE id_publicacion = :idPublicacion
            """, nativeQuery = true)
    int eliminarImagenesPublicacion(@Param("idPublicacion") Long idPublicacion);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            DELETE FROM colaborador_publicacion
            WHERE id_publicacion = :idPublicacion
            """, nativeQuery = true)
    int eliminarColaboradoresPublicacion(@Param("idPublicacion") Long idPublicacion);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            DELETE FROM colaborador_publicacion
            WHERE id_publicacion = :idPublicacion
              AND id_perfil = :idPerfil
            """, nativeQuery = true)
    int eliminarColaboradorPublicacion(
            @Param("idPublicacion") Long idPublicacion,
            @Param("idPerfil") Long idPerfil);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            DELETE FROM me_gusta
            WHERE id_publicacion = :idPublicacion
            """, nativeQuery = true)
    int eliminarMeGustaDePublicacion(@Param("idPublicacion") Long idPublicacion);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            DELETE FROM comentario
            WHERE id_publicacion = :idPublicacion
            """, nativeQuery = true)
    int eliminarComentariosDePublicacion(@Param("idPublicacion") Long idPublicacion);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            DELETE FROM publicacion
            WHERE id_publicacion = :idPublicacion
            """, nativeQuery = true)
    int eliminarFisicamente(@Param("idPublicacion") Long idPublicacion);

    /**
     * Coincidencias exactas por SHA-256 para la detección automática.
     */
    @Query(value = """
            SELECT p.id_publicacion AS idPublicacion,
                   i.id_imagen AS idImagen,
                   i.nombre_archivo AS nombreArchivo,
                   i.sha256_contenido AS sha256Contenido,
                   0 AS distancia
            FROM imagen i
            JOIN imagen_publicacion ip ON ip.id_imagen = i.id_imagen
            JOIN publicacion p ON p.id_publicacion = ip.id_publicacion
            JOIN perfil pf ON pf.id_perfil = p.id_perfil
            JOIN usuario u ON u.id_usuario = pf.id_usuario
            WHERE i.sha256_contenido = :sha256
              AND p.estado = 'ACTIVA'
              AND pf.estado = 'ACTIVO'
              AND u.estado = 'ACTIVO'
              AND p.id_publicacion <> :idPublicacionActual
              AND p.id_perfil <> :idPerfilActual
            ORDER BY p.fecha_publicacion DESC, p.id_publicacion DESC
            LIMIT :limite
            """, nativeQuery = true)
    List<org.mgroko.backend.repositorio.proyeccion.CoincidenciaImagenProyeccion>
            buscarCoincidenciasExactas(
                    @Param("sha256") String sha256,
                    @Param("idPublicacionActual") Long idPublicacionActual,
                    @Param("idPerfilActual") Long idPerfilActual,
                    @Param("idUsuarioActual") Long idUsuarioActual,
                    @Param("limite") int limite);

    /**
     * Coincidencias perceptuales por distancia de Hamming entre pHash.
     */
    @Query(value = """
            SELECT p.id_publicacion AS idPublicacion,
                   i.id_imagen AS idImagen,
                   i.nombre_archivo AS nombreArchivo,
                   i.sha256_contenido AS sha256Contenido,
                   bit_count(CAST(i.phash AS bit(64)) # CAST(:phash AS bit(64))) AS distancia
            FROM imagen i
            JOIN imagen_publicacion ip ON ip.id_imagen = i.id_imagen
            JOIN publicacion p ON p.id_publicacion = ip.id_publicacion
            JOIN perfil pf ON pf.id_perfil = p.id_perfil
            JOIN usuario u ON u.id_usuario = pf.id_usuario
            WHERE i.phash IS NOT NULL
              AND bit_count(CAST(i.phash AS bit(64)) # CAST(:phash AS bit(64))) <= :umbral
              AND p.estado = 'ACTIVA'
              AND pf.estado = 'ACTIVO'
              AND u.estado = 'ACTIVO'
              AND p.id_publicacion <> :idPublicacionActual
              AND p.id_perfil <> :idPerfilActual
            ORDER BY distancia ASC, p.fecha_publicacion DESC
            LIMIT :limite
            """, nativeQuery = true)
    List<org.mgroko.backend.repositorio.proyeccion.CoincidenciaImagenProyeccion>
            buscarCoincidenciasPerceptuales(
                    @Param("phash") Long phash,
                    @Param("umbral") int umbral,
                    @Param("idPublicacionActual") Long idPublicacionActual,
                    @Param("idPerfilActual") Long idPerfilActual,
                    @Param("idUsuarioActual") Long idUsuarioActual,
                    @Param("limite") int limite);

    /**
     * Publicaciones históricas a procesar, en orden de ID para paginación por
     * clave.
     */
    @Query("""
            SELECT p.idPublicacion
            FROM Publicacion p
            WHERE p.estado = org.mgroko.backend.modelo.enums.EstadoPublicacion.ACTIVA
              AND p.perfil.estado = org.mgroko.backend.modelo.enums.EstadoPerfil.ACTIVO
              AND p.perfil.usuario.estado = org.mgroko.backend.modelo.enums.EstadoUsuario.ACTIVO
              AND p.idPublicacion = COALESCE(:idPublicacion, p.idPublicacion)
              AND p.perfil.idPerfil = COALESCE(:idPerfil, p.perfil.idPerfil)
              AND p.fechaPublicacion >= COALESCE(:desde, p.fechaPublicacion)
              AND p.fechaPublicacion <= COALESCE(:hasta, p.fechaPublicacion)
              AND p.idPublicacion > :ultimoId
            ORDER BY p.idPublicacion
            """)
    List<Long> buscarIdsPublicacionesHistoricas(
            @Param("idPublicacion") Long idPublicacion,
            @Param("idPerfil") Long idPerfil,
            @Param("desde") java.time.LocalDateTime desde,
            @Param("hasta") java.time.LocalDateTime hasta,
            @Param("ultimoId") Long ultimoId,
            Pageable pageable);

    @Query("""
            SELECT COUNT(p)
            FROM Publicacion p
            WHERE p.estado = org.mgroko.backend.modelo.enums.EstadoPublicacion.ACTIVA
              AND p.perfil.estado = org.mgroko.backend.modelo.enums.EstadoPerfil.ACTIVO
              AND p.perfil.usuario.estado = org.mgroko.backend.modelo.enums.EstadoUsuario.ACTIVO
              AND p.idPublicacion = COALESCE(:idPublicacion, p.idPublicacion)
              AND p.perfil.idPerfil = COALESCE(:idPerfil, p.perfil.idPerfil)
              AND p.fechaPublicacion >= COALESCE(:desde, p.fechaPublicacion)
              AND p.fechaPublicacion <= COALESCE(:hasta, p.fechaPublicacion)
            """)
    long contarPublicacionesHistoricas(
            @Param("idPublicacion") Long idPublicacion,
            @Param("idPerfil") Long idPerfil,
            @Param("desde") java.time.LocalDateTime desde,
            @Param("hasta") java.time.LocalDateTime hasta);

    @EntityGraph(attributePaths = {
            "perfil",
            "perfil.usuario",
            "imagenes"
    })
    @Query("SELECT p FROM Publicacion p WHERE p.idPublicacion = :id")
    Optional<Publicacion> findByIdPublicacionConImagenes(@Param("id") Long id);

    @Query(value = """
            SELECT COUNT(*)
            FROM imagen i
            JOIN imagen_publicacion ip ON ip.id_imagen = i.id_imagen
            JOIN publicacion p ON p.id_publicacion = ip.id_publicacion
            JOIN perfil pf ON pf.id_perfil = p.id_perfil
            JOIN usuario u ON u.id_usuario = pf.id_usuario
            WHERE p.estado = 'ACTIVA'
              AND pf.estado = 'ACTIVO'
              AND u.estado = 'ACTIVO'
              AND p.id_publicacion = COALESCE(:idPublicacion, p.id_publicacion)
              AND p.id_perfil = COALESCE(:idPerfil, p.id_perfil)
              AND p.fecha_publicacion >= COALESCE(:desde, p.fecha_publicacion)
              AND p.fecha_publicacion <= COALESCE(:hasta, p.fecha_publicacion)
            """, nativeQuery = true)
    long contarImagenesHistoricas(
            @Param("idPublicacion") Long idPublicacion,
            @Param("idPerfil") Long idPerfil,
            @Param("desde") java.time.LocalDateTime desde,
            @Param("hasta") java.time.LocalDateTime hasta);

    @Query(value = """
            SELECT COUNT(*)
            FROM imagen i
            JOIN imagen_publicacion ip ON ip.id_imagen = i.id_imagen
            JOIN publicacion p ON p.id_publicacion = ip.id_publicacion
            JOIN perfil pf ON pf.id_perfil = p.id_perfil
            JOIN usuario u ON u.id_usuario = pf.id_usuario
            WHERE p.estado = 'ACTIVA'
              AND pf.estado = 'ACTIVO'
              AND u.estado = 'ACTIVO'
              AND i.sha256_contenido IS NULL
              AND p.id_publicacion = COALESCE(:idPublicacion, p.id_publicacion)
              AND p.id_perfil = COALESCE(:idPerfil, p.id_perfil)
              AND p.fecha_publicacion >= COALESCE(:desde, p.fecha_publicacion)
              AND p.fecha_publicacion <= COALESCE(:hasta, p.fecha_publicacion)
            """, nativeQuery = true)
    long contarImagenesSinSha256Historicas(
            @Param("idPublicacion") Long idPublicacion,
            @Param("idPerfil") Long idPerfil,
            @Param("desde") java.time.LocalDateTime desde,
            @Param("hasta") java.time.LocalDateTime hasta);

    @Query(value = """
            SELECT COUNT(*)
            FROM imagen i
            JOIN imagen_publicacion ip ON ip.id_imagen = i.id_imagen
            JOIN publicacion p ON p.id_publicacion = ip.id_publicacion
            JOIN perfil pf ON pf.id_perfil = p.id_perfil
            JOIN usuario u ON u.id_usuario = pf.id_usuario
            WHERE p.estado = 'ACTIVA'
              AND pf.estado = 'ACTIVO'
              AND u.estado = 'ACTIVO'
              AND i.phash IS NULL
              AND p.id_publicacion = COALESCE(:idPublicacion, p.id_publicacion)
              AND p.id_perfil = COALESCE(:idPerfil, p.id_perfil)
              AND p.fecha_publicacion >= COALESCE(:desde, p.fecha_publicacion)
              AND p.fecha_publicacion <= COALESCE(:hasta, p.fecha_publicacion)
            """, nativeQuery = true)
    long contarImagenesSinPhashHistoricas(
            @Param("idPublicacion") Long idPublicacion,
            @Param("idPerfil") Long idPerfil,
            @Param("desde") java.time.LocalDateTime desde,
            @Param("hasta") java.time.LocalDateTime hasta);

    @Query(value = """
            SELECT COUNT(*)
            FROM imagen i1
            JOIN imagen_publicacion ip1 ON ip1.id_imagen = i1.id_imagen
            JOIN publicacion p1 ON p1.id_publicacion = ip1.id_publicacion
            JOIN perfil pf1 ON pf1.id_perfil = p1.id_perfil
            JOIN usuario u1 ON u1.id_usuario = pf1.id_usuario
            JOIN imagen i2 ON i2.id_imagen > i1.id_imagen
            JOIN imagen_publicacion ip2 ON ip2.id_imagen = i2.id_imagen
            JOIN publicacion p2 ON p2.id_publicacion = ip2.id_publicacion
            JOIN perfil pf2 ON pf2.id_perfil = p2.id_perfil
            JOIN usuario u2 ON u2.id_usuario = pf2.id_usuario
            WHERE i1.phash IS NOT NULL
              AND i2.phash IS NOT NULL
              AND p1.id_publicacion <> p2.id_publicacion
              AND p1.id_perfil <> p2.id_perfil
              AND p1.estado = 'ACTIVA' AND pf1.estado = 'ACTIVO' AND u1.estado = 'ACTIVO'
              AND p2.estado = 'ACTIVA' AND pf2.estado = 'ACTIVO' AND u2.estado = 'ACTIVO'
              AND (p1.id_publicacion = COALESCE(:idPublicacion, p1.id_publicacion)
                   OR p2.id_publicacion = COALESCE(:idPublicacion, p2.id_publicacion))
              AND (p1.id_perfil = COALESCE(:idPerfil, p1.id_perfil)
                   OR p2.id_perfil = COALESCE(:idPerfil, p2.id_perfil))
              AND (p1.fecha_publicacion >= COALESCE(:desde, p1.fecha_publicacion)
                   OR p2.fecha_publicacion >= COALESCE(:desde, p2.fecha_publicacion))
              AND (p1.fecha_publicacion <= COALESCE(:hasta, p1.fecha_publicacion)
                   OR p2.fecha_publicacion <= COALESCE(:hasta, p2.fecha_publicacion))
              AND bit_count(CAST(i1.phash AS bit(64)) # CAST(i2.phash AS bit(64))) <= :umbral
            """, nativeQuery = true)
    long contarParesCandidatosHistoricos(
            @Param("idPublicacion") Long idPublicacion,
            @Param("idPerfil") Long idPerfil,
            @Param("desde") java.time.LocalDateTime desde,
            @Param("hasta") java.time.LocalDateTime hasta,
            @Param("umbral") int umbral);
}
