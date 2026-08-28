package org.mgroko.backend.repositorio;

import java.util.List;

import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.enums.EstadoPerfil;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerfilRepository extends JpaRepository<Perfil, Long> {

    /**
     * Busca todos los perfiles asociados a un usuario por su ID.
     *
     * @param idUsuario el ID del usuario
     * @return lista de perfiles del usuario
     */
    List<Perfil> findByUsuarioIdUsuario(Long idUsuario);

    /**
     * Indica si el usuario ya posee un perfil (para la profesión indicada)
     * en un estado distinto al pasado por parámetro.
     *
     * @param idUsuario    el ID del usuario
     * @param idProfesion  el ID de la profesión
     * @param estado       estado a excluir (por ejemplo {@code EstadoPerfil.Baja})
     * @return true si existe al menos un perfil del usuario en esa profesión con otro estado
     */
    boolean existsByUsuarioIdUsuarioAndProfesionIdProfesionAndEstadoNot(Long idUsuario, Long idProfesion, EstadoPerfil estado);
}
