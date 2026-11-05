package org.mgroko.backend.modelo;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * UC-37: calificación y reseña que un perfil emisor deja a otro perfil
 * receptor por su participación en un proyecto.
 */
@Entity
@Table(name = "calificacion_perfil")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CalificacionPerfil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_calificacion")
    private Long idCalificacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proyecto", nullable = false)
    private Proyecto proyecto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_perfil_emisor", nullable = false)
    private Perfil perfilEmisor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_perfil_receptor", nullable = false)
    private Perfil perfilReceptor;

    /**
     * Valoración de 1 (muy mala) a 5 (excelente).
     */
    @Column(name = "valoracion", nullable = false)
    private Integer valoracion;

    @Column(name = "resenia", nullable = false, length = 200)
    private String resenia;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @PrePersist
    private void prepararFechaRegistro() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
    }
}
