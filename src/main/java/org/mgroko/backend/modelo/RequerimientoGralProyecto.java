package org.mgroko.backend.modelo;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "requerimiento_gral_proyecto")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RequerimientoGralProyecto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_requerimiento_gral")
    private Long idRequerimientoGral;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proyecto", nullable = false)
    private Proyecto proyecto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_profesion", nullable = false)
    private Profesion profesion;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "requerimiento_gral_habilidad",
        joinColumns = @JoinColumn(name = "id_requerimiento_gral"),
        inverseJoinColumns = @JoinColumn(name = "id_habilidad")
    )
    private Set<Habilidad> habilidades = new HashSet<>();

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "requerimiento_gral_caract",
        joinColumns = @JoinColumn(name = "id_requerimiento_gral"),
        inverseJoinColumns = @JoinColumn(name = "id_caracteristica")
    )
    private Set<CaracteristicaTecnica> caracteristicas = new HashSet<>();
}
