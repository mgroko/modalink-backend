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

    // Dato real de entrada (no derivado) — define cuánto dura la tarea.
    @Column(name = "duracion_minutos", nullable = false)
    private Integer duracionMinutos;

    // Calculado por el procedure de asignación en base a disponibilidad
    // del staff, pero se guarda como columna real (no es una expresión
    // trivial de la misma fila).
    @Column(name = "fecha_hora_inicio", nullable = false)
    private LocalDateTime fechaHoraInicio;

    // Columna GENERATED ALWAYS AS en la base (fecha_hora_inicio + duracion).
    // insertable/updatable = false: Hibernate no debe intentar escribirla,
    // Postgres la calcula sola. Después de persistir, si necesitás verla
    // ya resuelta en la misma transacción, hacé un refresh/reload de la entidad.
    @Column(name = "fecha_hora_fin", insertable = false, updatable = false)
    private LocalDateTime fechaHoraFin;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_planificacion", nullable = false)
    private Planificacion planificacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ubicacion")
    private Ubicacion ubicacion;

    // Relación auto-referenciada M:N a través de dependencia_actividades.
    // "predecesoras" es el lado propietario; "sucesoras" es su espejo de
    // solo lectura (insertable/updatable = false) para no duplicar la
    // gestión de la misma tabla intermedia desde los dos lados.
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
