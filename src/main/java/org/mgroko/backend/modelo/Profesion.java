package org.mgroko.backend.modelo;

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

    @Column(name = "codigo", nullable = false, length = 50, unique = true)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 50, unique = true)
    private String nombre;

    /**
     * Forma femenina del nombre de la profesión. Puede ser nula o igual a
     * {@link #nombre} cuando la palabra es invariable (por ejemplo, "Modelo").
     */
    @Column(name = "nombre_femenino", length = 50)
    private String nombreFemenino;

    @Column(name = "descripcion", length = 200)
    private String descripcion;
}
