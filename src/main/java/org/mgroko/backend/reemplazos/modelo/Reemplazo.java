package org.mgroko.backend.reemplazos.modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.mgroko.backend.modelo.Actividad;
import org.mgroko.backend.modelo.MiembroProyecto;
import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Proyecto;
import org.mgroko.backend.modelo.RequerimientoActividad;
import org.mgroko.backend.modelo.RequerimientoGralProyecto;
import org.mgroko.backend.reemplazos.enums.EstadoReemplazo;
import org.mgroko.backend.reemplazos.enums.OrigenReemplazo;
import org.mgroko.backend.reemplazos.enums.TipoReemplazo;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Proceso de reemplazo de una vacante puntual.
 *
 * <p>Puede representar una vacante de una actividad concreta o una vacante de
 * un requerimiento general del proyecto. En ambos casos conserva la membresía
 * histórica que dejó la vacante y el resultado de la incorporación.</p>
 */
@Entity
@Table(name = "reemplazo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reemplazo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reemplazo")
    private Long idReemplazo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoReemplazo tipo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proyecto", nullable = false)
    private Proyecto proyecto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_actividad")
    private Actividad actividad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_requerimiento_act")
    private RequerimientoActividad requerimientoActividad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_requerimiento_gral")
    private RequerimientoGralProyecto requerimientoGral;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_miembro_baja", nullable = false)
    private MiembroProyecto miembroBaja;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_perfil_baja", nullable = false)
    private Perfil perfilBaja;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen", nullable = false, length = 30)
    private OrigenReemplazo origen;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 40)
    private EstadoReemplazo estado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(name = "id_miembro_resultante")
    private Long idMiembroResultante;

    @Column(name = "id_asignacion_resultante")
    private Long idAsignacionResultante;

    @Builder.Default
    @OneToMany(mappedBy = "reemplazo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReemplazoCandidato> candidatos = new ArrayList<>();

    @PrePersist
    private void prepararFechaCreacion() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}
