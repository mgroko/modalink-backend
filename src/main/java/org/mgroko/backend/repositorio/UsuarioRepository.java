package org.mgroko.backend.repositorio;

import java.time.LocalDateTime;
import java.util.List;
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

    /**
     * Busca los usuarios en un estado cuya fecha de solicitud de baja sea
     * anterior a la fecha pasada por parámetro. Se usa para expirar cuentas
     * pendientes de baja pasados los 30 días (UC-07).
     *
     * @param estado      estado a buscar (por ejemplo {@code EstadoUsuario.PendienteBaja})
     * @param fechaLimite fecha límite para considerar la cuenta vencida
     * @return lista de usuarios vencidos
     */
    List<Usuario> findByEstadoAndFechaSolicitudBajaBefore(EstadoUsuario estado, LocalDateTime fechaLimite);

    /**
     * Busca los usuarios deshabilitados cuya deshabilitación venció antes de
     * la fecha pasada por parámetro. Se usa para reactivar automáticamente
     * las cuentas con duración definida (UC-04).
     *
     * @param estado      estado a buscar (por ejemplo {@code EstadoUsuario.Deshabilitado})
     * @param fechaLimite fecha límite para considerar vencida la deshabilitación
     * @return lista de usuarios con deshabilitación vencida
     */
    List<Usuario> findByEstadoAndFechaHastaDeshabilitacionBefore(EstadoUsuario estado, LocalDateTime fechaLimite);

}
