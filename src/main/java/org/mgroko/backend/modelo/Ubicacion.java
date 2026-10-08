package org.mgroko.backend.modelo;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ubicacion")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Ubicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ubicacion")
    private Long idUbicacion;

    @Column(name = "direccion", length = 100)
    private String direccion;

    @Column(name = "codigo_postal", length = 10)
    private String codigoPostal;

    @Column(name = "latitud", precision = 10, scale = 8)
    private BigDecimal latitud;

    @Column(name = "longitud", precision = 11, scale = 8)
    private BigDecimal longitud;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_ciudad", nullable = false)
    private Ciudad ciudad;
}
