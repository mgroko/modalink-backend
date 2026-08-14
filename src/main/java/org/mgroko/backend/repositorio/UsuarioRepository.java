package org.mgroko.backend.repositorio;

import java.util.Optional;

import org.mgroko.backend.modelo.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
