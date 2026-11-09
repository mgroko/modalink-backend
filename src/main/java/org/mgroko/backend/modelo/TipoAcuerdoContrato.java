package org.mgroko.backend.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tipo de acuerdo que puede usarse para generar un contrato (UC-39).
 */
@Entity
@Table(name = "tipo_acuerdo_contrato")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TipoAcuerdoContrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_acuerdo")
    private Long idTipoAcuerdo;

    @Column(name = "codigo", nullable = false, length = 50, unique = true)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    /**
     * Plantilla legal con placeholders {{proyecto}}, {{director}}, {{partes}}
     * y {{clausulas_adicionales}}.
     */
    @Column(name = "plantilla", columnDefinition = "text")
    private String plantilla;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
