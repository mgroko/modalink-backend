package org.mgroko.backend.repositorio;

import java.util.List;

import org.mgroko.backend.modelo.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerfilRepository extends JpaRepository<Perfil, Long> {

    /**
     * Busca todos los perfiles asociados a un usuario por su ID.
     *
     * @param idUsuario el ID del usuario
     * @return lista de perfiles del usuario
     */
    List<Perfil> findByUsuarioIdUsuario(Long idUsuario);
}
