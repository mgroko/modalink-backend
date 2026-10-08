package org.mgroko.backend.modelo;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "unidad_medida")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UnidadMedida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_unidad")
    private Long idUnidad;

    @Column(name = "nombre", length = 50)
    private String nombre;

    @Column(name = "simbolo", length = 50)
    private String simbolo;

    @Column(name = "tipo_dato_permitido", length = 50)
    private String tipoDatoPermitido;
}
