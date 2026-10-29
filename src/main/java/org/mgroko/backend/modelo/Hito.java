package org.mgroko.backend.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mapeo mínimo del hito para resolver el proyecto de las notificaciones
 * {@code HITO_CUMPLIDO}. No se expone como recurso ni se modifica desde la API.
 */
@Entity
@Table(name = "hito")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Hito {

    @Id
    @Column(name = "id_hito")
    private Long idHito;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proyecto", nullable = false)
    private Proyecto proyecto;
}
