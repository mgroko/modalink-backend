package org.mgroko.backend.modelo;

import org.mgroko.backend.modelo.enums.EstadoUsuario;
import org.mgroko.backend.modelo.enums.ProveedorAuth;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;

    @Column(name = "apellido", nullable = false, length = 50)
    private String apellido;

    @Column(name = "dni", nullable = false, length = 15, unique = true)
    private String dni;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(name = "correo", nullable = false, length = 255, unique = true)
    private String correo;

    // null si el usuario solo se registró vía Google
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "proveedor_auth", nullable = false, length = 20)
    @Builder.Default
    private ProveedorAuth proveedorAuth = ProveedorAuth.LOCAL;

    // "sub" devuelto por Google; null si proveedor_auth = LOCAL
    @Column(name = "id_externo", length = 100)
    private String idExterno;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoUsuario estado = EstadoUsuario.Activo;

    @Column(name = "fecha_solicitud_baja")
    private LocalDateTime fechaSolicitudBaja;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ubicacion")
    private Ubicacion ubicacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_rol_global", nullable = false)
    private RolGlobal rolGlobal;
}
