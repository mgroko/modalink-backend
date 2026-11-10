package org.mgroko.backend.repositorio;

import java.util.Optional;

import org.mgroko.backend.modelo.Conversacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ConversacionRepository extends JpaRepository<Conversacion, Long> {

    @EntityGraph(attributePaths = {
            "perfil1",
            "perfil1.usuario",
            "perfil1.profesion",
            "perfil1.imagen",
            "perfil2",
            "perfil2.usuario",
            "perfil2.profesion",
            "perfil2.imagen"
    })
    @Query("""
        SELECT c
        FROM Conversacion c
        WHERE (c.perfil1.idPerfil = :idPerfil OR c.perfil2.idPerfil = :idPerfil)
          AND c.fechaUltimoMensaje IS NOT NULL
    """)
    Page<Conversacion> buscarDePerfil(
            @Param("idPerfil") Long idPerfil,
            Pageable pageable);

    @Query("""
        SELECT COUNT(c)
        FROM Conversacion c
        WHERE (c.perfil1.idPerfil = :idPerfil OR c.perfil2.idPerfil = :idPerfil)
          AND c.fechaUltimoMensaje IS NOT NULL
    """)
    long contarDePerfil(@Param("idPerfil") Long idPerfil);

    @EntityGraph(attributePaths = {
            "perfil1",
            "perfil1.usuario",
            "perfil1.profesion",
            "perfil1.imagen",
            "perfil2",
            "perfil2.usuario",
            "perfil2.profesion",
            "perfil2.imagen"
    })
    @Query("""
        SELECT c
        FROM Conversacion c
        WHERE c.perfil1.idPerfil = :idPerfil1
          AND c.perfil2.idPerfil = :idPerfil2
    """)
    Optional<Conversacion> buscarPorPar(
            @Param("idPerfil1") Long idPerfil1,
            @Param("idPerfil2") Long idPerfil2);

    @EntityGraph(attributePaths = {
            "perfil1",
            "perfil1.usuario",
            "perfil1.profesion",
            "perfil1.imagen",
            "perfil2",
            "perfil2.usuario",
            "perfil2.profesion",
            "perfil2.imagen"
    })
    @Query("""
        SELECT c
        FROM Conversacion c
        WHERE c.idConversacion = :idConversacion
    """)
    Optional<Conversacion> buscarPorIdConPerfiles(
            @Param("idConversacion") Long idConversacion);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT c
        FROM Conversacion c
        WHERE c.idConversacion = :idConversacion
    """)
    Optional<Conversacion> findByIdForUpdate(
            @Param("idConversacion") Long idConversacion);
}
