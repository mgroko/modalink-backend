package org.mgroko.backend.repositorio;

import org.mgroko.backend.modelo.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
}
