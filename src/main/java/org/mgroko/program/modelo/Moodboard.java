package org.mgroko.program.modelo;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "moodboard")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Moodboard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_moodboard")
    private Long idMoodboard;

    @Column(name = "fecha_creacion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "descripcion", length = 300)
    private String descripcion;

    // 1 a 1 con proyecto (UNIQUE en la base)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proyecto", nullable = false, unique = true)
    private Proyecto proyecto;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "imagen_moodboard",
        joinColumns = @JoinColumn(name = "id_moodboard"),
        inverseJoinColumns = @JoinColumn(name = "id_imagen")
    )
    private Set<Imagen> imagenes = new HashSet<>();
}
