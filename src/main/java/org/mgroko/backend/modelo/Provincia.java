package org.mgroko.backend.modelo;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "provincia")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Provincia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_provincia")
    private Long idProvincia;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "id_externo", length = 50)
    private String idExterno;

    @Column(name = "fuente_api", length = 50)
    private String fuenteApi;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pais", nullable = false)
    private Pais pais;
}
