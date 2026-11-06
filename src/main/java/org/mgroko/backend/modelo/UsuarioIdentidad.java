package org.mgroko.backend.modelo;

import java.time.LocalDateTime;

import org.mgroko.backend.modelo.enums.ProveedorAuth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Identidad de autenticación asociada a un usuario.
 *
 * <p>Un usuario puede tener una identidad LOCAL (correo + contraseña) y/o una
 * identidad GOOGLE (sub de Google). La cuenta canónica sigue siendo
 * {@link Usuario}; esta tabla solo resuelve con qué proveedor se autenticó.</p>
 */
@Entity
@Table(name = "usuario_identidad", uniqueConstraints = {
        @UniqueConstraint(name = "uq_identidad_proveedor_externo", columnNames = { "proveedor", "id_externo" }),
        @UniqueConstraint(name = "uq_identidad_usuario_proveedor", columnNames = { "id_usuario", "proveedor" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioIdentidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_identidad")
    private Long idIdentidad;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "proveedor", nullable = false, length = 20)
    private ProveedorAuth proveedor;

    // "sub" de Google. Null para identidades LOCAL.
    @Column(name = "id_externo", length = 255)
    private String idExterno;

    @Column(name = "correo", nullable = false, length = 255)
    private String correo;

    @Column(name = "correo_verificado", nullable = false)
    @Builder.Default
    private boolean correoVerificado = false;

    @Column(name = "fecha_vinculacion", nullable = false)
    private LocalDateTime fechaVinculacion;

    @PrePersist
    void completarFechaVinculacion() {
        if (fechaVinculacion == null) {
            fechaVinculacion = LocalDateTime.now();
        }
    }
}
