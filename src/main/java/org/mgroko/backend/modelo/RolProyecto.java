package org.mgroko.backend.modelo;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "rol_proyecto")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RolProyecto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol_proyecto")
    private Long idRolProyecto;

    @Column(name = "nombre", nullable = false, length = 20, unique = true)
    private String nombre;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "rol_proyecto_permiso",
        joinColumns = @JoinColumn(name = "id_rol_proyecto"),
        inverseJoinColumns = @JoinColumn(name = "id_permiso_proyecto")
    )
    private Set<PermisoProyecto> permisos = new HashSet<>();
}
