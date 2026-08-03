package org.mgroko.backend.modelo;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "requerimiento_actividad")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RequerimientoActividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_requerimiento")
    private Long idRequerimiento;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_profesion", nullable = false)
    private Profesion profesion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_actividad", nullable = false)
    private Actividad actividad;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "requerimiento_act_habilidad",
        joinColumns = @JoinColumn(name = "id_requerimiento"),
        inverseJoinColumns = @JoinColumn(name = "id_habilidad")
    )
    private Set<Habilidad> habilidades = new HashSet<>();

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "requerimiento_act_caract",
        joinColumns = @JoinColumn(name = "id_requerimiento"),
        inverseJoinColumns = @JoinColumn(name = "id_caracteristica")
    )
    private Set<CaracteristicaTecnica> caracteristicas = new HashSet<>();
}
