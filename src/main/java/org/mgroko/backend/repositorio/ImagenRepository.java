package org.mgroko.backend.repositorio;

import org.mgroko.backend.modelo.Imagen;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImagenRepository extends JpaRepository<Imagen, Long> {
}