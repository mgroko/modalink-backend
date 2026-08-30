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

    // id de la localidad en el catálogo de Georef (para prellenar el selector)
    @Column(name = "id_georef", length = 20)
    private String idGeoref;

    @Column(name = "localidad", nullable = false, length = 100)
    private String localidad;

    @Column(name = "pais", length = 50)
    private String pais;

    @Column(name = "provincia", nullable = false, length = 100)
    private String provincia;

    @Column(name = "codigo_postal", length = 10)
    private String codigoPostal;

    @Column(name = "latitud", precision = 10, scale = 8)
    private BigDecimal latitud;

    @Column(name = "longitud", precision = 11, scale = 8)
    private BigDecimal longitud;
}
