package org.mgroko.backend.repositorio;

import java.util.Optional;

import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Busca un usuario por su correo electrónico.
     *
     * @param correo el correo a buscar
     * @return un Optional con el Usuario si existe, vacío en caso contrario
     */
    Optional<Usuario> findByCorreo(String correo);

    /**
     * Verifica si existe un usuario con el correo especificado.
     *
     * @param correo el correo a verificar
     * @return true si existe, false en caso contrario
     */
    boolean existsByCorreo(String correo);

    /**
     * Verifica si existe un usuario con el DNI especificado.
     *
     * @param dni el DNI a verificar
     * @return true si existe, false en caso contrario
     */
    boolean existsByDni(String dni);

    /**
     * Trae únicamente el estado de un usuario por su id, sin hidratar el
     * resto de la entidad (rolGlobal, genero, ubicacion). Pensado para
     * consultarse en cada request autenticado desde JwtAuthenticationFilter.
     *
     * @param idUsuario el id del usuario
     * @return un Optional con el EstadoUsuario si el usuario existe, vacío en caso contrario
     */
    @Query("SELECT u.estado FROM Usuario u WHERE u.idUsuario = :idUsuario")
    Optional<EstadoUsuario> findEstadoByIdUsuario(@Param("idUsuario") Long idUsuario);

}
