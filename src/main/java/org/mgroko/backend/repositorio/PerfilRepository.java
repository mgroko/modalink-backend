package org.mgroko.backend.repositorio;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    /**
     * Busca un perfil por su ID siempre que pertenezca al usuario indicado.
     * Evita que un usuario acceda a perfiles de otros.
     *
     * @param idPerfil  el ID del perfil
     * @param idUsuario el ID del usuario dueño del perfil
     * @return el perfil si existe y pertenece al usuario
     */
    Optional<Perfil> findByIdPerfilAndUsuarioIdUsuario(Long idPerfil, Long idUsuario);

    /**
     * Busca los perfiles en un estado cuya fecha de solicitud de baja sea
     * anterior a la fecha pasada por parámetro. Se usa para expirar perfiles
     * pendientes de baja pasados los 30 días (UC-12).
     *
     * @param estado              estado a buscar (por ejemplo {@code EstadoPerfil.PendienteBaja})
     * @param fechaLimite         fecha límite para considerar el perfil vencido
     * @return lista de perfiles vencidos
     */
    List<Perfil> findByEstadoAndFechaSolicitudBajaBefore(EstadoPerfil estado, LocalDateTime fechaLimite);

    /**
     * Busca perfiles de un usuario en un estado específico.
     *
     * @param idUsuario el ID del usuario
     * @param estado    el estado a buscar
     * @return lista de perfiles del usuario en el estado especificado
     */
    List<Perfil> findByUsuario_IdUsuarioAndEstado(Long idUsuario, EstadoPerfil estado);
}
