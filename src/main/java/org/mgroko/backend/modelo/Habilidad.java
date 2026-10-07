package org.mgroko.backend.modelo;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "habilidad")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Habilidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_habilidad")
    private Long idHabilidad;

    @Column(name = "codigo", nullable = false, length = 50)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 255, unique = true)
    private String nombre;

    @Column(name = "descripcion", length = 255)
    private String descripcion;
}
