package org.mgroko.program.modelo;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "rol_global")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RolGlobal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol_global")
    private Long idRolGlobal;

    @Column(name = "nombre", nullable = false, length = 20, unique = true)
    private String nombre;

    // Catálogo puro: rol_global_permiso no tiene atributos propios,
    // se modela como M:N directa en vez de una clase intermedia.
    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "rol_global_permiso",
        joinColumns = @JoinColumn(name = "id_rol_global"),
        inverseJoinColumns = @JoinColumn(name = "id_permiso_global")
    )
    private Set<PermisoGlobal> permisos = new HashSet<>();
}
