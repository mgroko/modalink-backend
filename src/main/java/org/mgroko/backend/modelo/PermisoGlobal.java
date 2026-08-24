package org.mgroko.backend.modelo;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permiso_global")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PermisoGlobal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_permiso_global")
    private Long idPermisoGlobal;

    @Column(name = "nombre", nullable = false, length = 50, unique = true)
    private String nombre;
}
