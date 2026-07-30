package org.mgroko.program.modelo;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "objetivo")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Objetivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_objetivo")
    private Long idObjetivo;

    @Column(name = "nombre", length = 100)
    private String nombre;

    @Column(name = "descripcion", length = 300)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proyecto", nullable = false)
    private Proyecto proyecto;
}
