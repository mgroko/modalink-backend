package org.mgroko.backend.modelo;

import jakarta.persistence.*;
import lombok.*;
import org.mgroko.backend.modelo.enums.EstadoActividad;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 50)
    @Builder.Default
    private EstadoActividad estado = EstadoActividad.PENDIENTE;

    @Column(name = "nombre", nullable = false, length = 20)
    private String nombre;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    @Column(name = "duracion_minutos", nullable = false)
    private Integer duracionMinutos;

    @Column(name = "fecha_hora_inicio", nullable = false)
    private LocalDateTime fechaHoraInicio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_planificacion", nullable = false)
    private Planificacion planificacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_ubicacion", nullable = false)
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

    public LocalDateTime getFechaHoraFin() {
        if (fechaHoraInicio == null || duracionMinutos == null) {
            return null;
        }
        return fechaHoraInicio.plusMinutes(duracionMinutos);
    }

    public void setFechaHoraFin(LocalDateTime fechaHoraFin) {
        if (fechaHoraInicio != null && fechaHoraFin != null) {
            this.duracionMinutos = (int) java.time.Duration.between(fechaHoraInicio, fechaHoraFin).toMinutes();
        }
    }

    public static class ActividadBuilder {
        public ActividadBuilder fechaHoraFin(LocalDateTime fechaHoraFin) {
            if (this.fechaHoraInicio != null && fechaHoraFin != null) {
                this.duracionMinutos = (int) java.time.Duration.between(this.fechaHoraInicio, fechaHoraFin).toMinutes();
            }
            return this;
        }
    }
}
