package org.mgroko.backend.repositorio;

import java.util.Optional;

import org.mgroko.backend.modelo.UnidadMedida;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnidadMedidaRepository extends JpaRepository<UnidadMedida, Long> {
    
    Optional<UnidadMedida> findBySimbolo(String simbolo);
    
    Optional<UnidadMedida> findByNombre(String nombre);
}