package org.mgroko.backend.repositorio;

import java.util.List;

import org.mgroko.backend.modelo.GrupoPolaroid;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GrupoPolaroidRepository extends JpaRepository<GrupoPolaroid, Long> {

    List<GrupoPolaroid> findAllByOrderByIdGrupoPolaroidAsc();
}
