package org.mgroko.backend.repositorio;

import java.util.List;
import java.util.Optional;

import org.mgroko.backend.modelo.Conexion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ConexionRepository extends JpaRepository<Conexion, Long> {

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
}
