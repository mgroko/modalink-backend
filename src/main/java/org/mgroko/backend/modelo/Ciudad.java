package org.mgroko.backend.modelo;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ciudad")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Ciudad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ciudad")
    private Long idCiudad;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "codigo_postal", length = 10)
    private String codigoPostal;

    @Column(name = "id_externo", length = 50)
    private String idExterno;

    @Column(name = "fuente_api", length = 50)
    private String fuenteApi;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "longitud_defecto", precision = 11, scale = 8)
    private BigDecimal longitudDefecto;

    @Column(name = "latitud_defecto", precision = 10, scale = 8)
    private BigDecimal latitudDefecto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_provincia", nullable = false)
    private Provincia provincia;
}
