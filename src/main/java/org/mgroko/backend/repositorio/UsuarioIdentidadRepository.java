package org.mgroko.backend.repositorio;

import java.util.Optional;

import org.mgroko.backend.modelo.UsuarioIdentidad;
import org.mgroko.backend.modelo.enums.ProveedorAuth;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioIdentidadRepository extends JpaRepository<UsuarioIdentidad, Long> {

    Optional<UsuarioIdentidad> findByProveedorAndIdExterno(ProveedorAuth proveedor, String idExterno);

    Optional<UsuarioIdentidad> findByUsuario_IdUsuarioAndProveedor(Long idUsuario, ProveedorAuth proveedor);

    boolean existsByProveedorAndIdExterno(ProveedorAuth proveedor, String idExterno);
}
