package org.mgroko.backend.modelo;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "caracteristica_tecnica")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CaracteristicaTecnica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_caracteristica")
    private Long idCaracteristica;

    @Column(name = "codigo", nullable = false, length = 50)
    private String codigo;

    @Column(name = "unidad", length = 50)
    private String unidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_profesion")
    private Profesion profesion;
}
