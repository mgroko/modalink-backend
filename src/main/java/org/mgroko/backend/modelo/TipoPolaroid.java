package org.mgroko.backend.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
 * Tipo seleccionable de polaroid: la combinación de un plano (grupo) y un
 * ángulo. La semilla inicial vive en la base de datos.
 */
@Entity
@Table(name = "tipo_polaroid")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoPolaroid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_polaroid")
    private Long idTipoPolaroid;

    @Column(name = "codigo", nullable = false, length = 50, unique = true)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_grupo_polaroid", nullable = false)
    private GrupoPolaroid grupo;
}
