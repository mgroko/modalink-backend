package org.mgroko.backend.repositorio;

import org.mgroko.backend.modelo.PostulacionGral;
import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostulacionGralRepository extends JpaRepository<PostulacionGral, Long> {

    boolean existsByRequerimientoGralIdRequerimientoGralAndEstado(
            Long idRequerimientoGral, EstadoSolicitud estado);
}
