package org.mgroko.program.modelo;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permiso_proyecto")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PermisoProyecto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_permiso_proyecto")
    private Long idPermisoProyecto;

    @Column(name = "nombre", nullable = false, length = 50, unique = true)
    private String nombre;
}
