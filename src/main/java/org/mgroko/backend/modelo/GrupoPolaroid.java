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
 * Agrupación visible de los tipos de polaroid: primer plano, plano americano
 * y cuerpo completo. La semilla inicial vive en la base de datos.
 */
@Entity
@Table(name = "grupo_polaroid")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrupoPolaroid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_grupo_polaroid")
    private Long idGrupoPolaroid;

    @Column(name = "codigo", nullable = false, length = 50, unique = true)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 100, unique = true)
    private String nombre;
}
