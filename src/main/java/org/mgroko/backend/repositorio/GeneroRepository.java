package org.mgroko.backend.repositorio;

import java.util.Optional;

import org.mgroko.backend.modelo.Genero;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeneroRepository extends JpaRepository<Genero, Long> {

    @org.springframework.data.jpa.repository.Query("SELECT g FROM Genero g WHERE UPPER(g.codigo) = UPPER(:codigo)")
    Optional<Genero> findByCodigo(@org.springframework.data.repository.query.Param("codigo") String codigo);

    Optional<Genero> findByCodigoIgnoreCase(String codigo);
}
