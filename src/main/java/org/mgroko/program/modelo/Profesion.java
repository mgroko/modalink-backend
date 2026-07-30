package org.mgroko.program.modelo;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "profesion")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Profesion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_profesion")
    private Long idProfesion;

    @Column(name = "nombre", nullable = false, length = 50, unique = true)
    private String nombre;

    @Column(name = "descripcion", length = 200)
    private String descripcion;
}
