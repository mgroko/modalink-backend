package org.mgroko.backend.repositorio;

import java.util.List;

import org.mgroko.backend.modelo.TipoPolaroid;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoPolaroidRepository extends JpaRepository<TipoPolaroid, Long> {

    @EntityGraph(attributePaths = "grupo")
    List<TipoPolaroid> findAllByOrderByIdTipoPolaroidAsc();
}
