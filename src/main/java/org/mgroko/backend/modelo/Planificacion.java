package org.mgroko.backend.modelo;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "planificacion")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Planificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_planificacion")
    private Long idPlanificacion;

    @Column(name = "fecha_entrega")
    private LocalDate fechaEntrega;

    // 1 a 1 con proyecto (UNIQUE en la base)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proyecto", nullable = false, unique = true)
    private Proyecto proyecto;
}
