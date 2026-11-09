package org.mgroko.backend.repositorio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.mgroko.backend.modelo.Conexion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ConexionRepository
        extends JpaRepository<Conexion, Long>,
                JpaSpecificationExecutor<Conexion> {

    /**
     * Indica si existe una conexión activa entre dos perfiles, sin importar el
     * orden en que se consulten.
     */
    @Query("""
        SELECT COUNT(c) > 0
        FROM Conexion c
        WHERE c.estado = org.mgroko.backend.modelo.enums.EstadoConexion.ACTIVA
          AND (
            (c.perfil1.idPerfil = :idPerfilA AND c.perfil2.idPerfil = :idPerfilB)
            OR
            (c.perfil1.idPerfil = :idPerfilB AND c.perfil2.idPerfil = :idPerfilA)
          )
    """)
    boolean existsActivaEntre(@Param("idPerfilA") Long idPerfilA,
            @Param("idPerfilB") Long idPerfilB);

    @Query("""
        SELECT c
        FROM Conexion c
        WHERE c.estado = org.mgroko.backend.modelo.enums.EstadoConexion.ACTIVA
          AND (
            (c.perfil1.idPerfil = :idPerfilA AND c.perfil2.idPerfil = :idPerfilB)
            OR
            (c.perfil1.idPerfil = :idPerfilB AND c.perfil2.idPerfil = :idPerfilA)
          )
    """)
    Optional<Conexion> findActivaEntre(@Param("idPerfilA") Long idPerfilA,
            @Param("idPerfilB") Long idPerfilB);

    /**
     * Red de contactos activa de un perfil. La consulta usa las dos columnas
     * del par canónico para no depender del orden en que se guardó.
     */
    @EntityGraph(attributePaths = {
            "perfil1",
            "perfil1.usuario",
            "perfil1.profesion",
            "perfil2",
            "perfil2.usuario",
            "perfil2.profesion"
    })
    @Query("""
        SELECT c
        FROM Conexion c
        WHERE c.estado = org.mgroko.backend.modelo.enums.EstadoConexion.ACTIVA
          AND (c.perfil1.idPerfil = :idPerfil OR c.perfil2.idPerfil = :idPerfil)
        ORDER BY c.fechaInicioConexion DESC
    """)
    List<Conexion> findActivasDePerfil(@Param("idPerfil") Long idPerfil);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT c
        FROM Conexion c
        WHERE c.idConexion = :idConexion
    """)
    Optional<Conexion> findByIdForUpdate(@Param("idConexion") Long idConexion);

    /**
     * Carga las conexiones de una página con sus perfiles, usuarios,
     * profesiones, imágenes y ciudad para evitar N+1 en UC-44 bis.
     */
    @EntityGraph(attributePaths = {
            "perfil1",
            "perfil1.usuario",
            "perfil1.usuario.ubicacion.ciudad.provincia.pais",
            "perfil1.profesion",
            "perfil1.imagen",
            "perfil2",
            "perfil2.usuario",
            "perfil2.usuario.ubicacion.ciudad.provincia.pais",
            "perfil2.profesion",
            "perfil2.imagen"
    })
    @Query("""
        SELECT c
        FROM Conexion c
        WHERE c.idConexion IN :ids
    """)
    List<Conexion> findAllByIdInWithPerfiles(
            @Param("ids") Collection<Long> ids);

    /**
     * Conteo de conexiones activas por profesión de la contraparte. Se usa en
     * el resumen de Mi red para los chips de profesión.
     */
    @Query(value = """
        SELECT p.id_profesion AS "idProfesion",
               pr.nombre AS "nombre",
               COUNT(*) AS "cantidad"
        FROM conexion c
        JOIN perfil p ON p.id_perfil = CASE
            WHEN c.id_perfil_1 = :idPerfil THEN c.id_perfil_2
            ELSE c.id_perfil_1
        END
        JOIN profesion pr ON pr.id_profesion = p.id_profesion
        WHERE c.estado = 'ACTIVA'
          AND (c.id_perfil_1 = :idPerfil OR c.id_perfil_2 = :idPerfil)
        GROUP BY p.id_profesion, pr.nombre
        ORDER BY pr.nombre
    """, nativeQuery = true)
    List<ConteoProfesionProjection> contarConexionesPorProfesion(
            @Param("idPerfil") Long idPerfil);

    interface ConteoProfesionProjection {
        Long getIdProfesion();

        String getNombre();

        Long getCantidad();
    }
}
