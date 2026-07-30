package org.mgroko.program.modelo;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "actividad")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Actividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_actividad")
    private Long idActividad;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    @Column(name = "duracion_minutos", nullable = false)
    private Integer duracionMinutos;

    @Column(name = "fecha_hora_inicio", nullable = false)
    private LocalDateTime fechaHoraInicio;

    @Column(name = "fecha_hora_fin", insertable = false, updatable = false)
    private LocalDateTime fechaHoraFin;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_planificacion", nullable = false)
    private Planificacion planificacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ubicacion")
    private Ubicacion ubicacion;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "dependencia_actividades",
        joinColumns = @JoinColumn(name = "id_actividad_sucesora"),
        inverseJoinColumns = @JoinColumn(name = "id_actividad_predecesora")
    )
    private Set<Actividad> predecesoras = new HashSet<>();

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "dependencia_actividades",
        joinColumns = @JoinColumn(name = "id_actividad_predecesora", insertable = false, updatable = false),
        inverseJoinColumns = @JoinColumn(name = "id_actividad_sucesora", insertable = false, updatable = false)
    )
    private Set<Actividad> sucesoras = new HashSet<>();
}
