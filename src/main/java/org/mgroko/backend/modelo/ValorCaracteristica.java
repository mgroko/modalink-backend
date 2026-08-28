package org.mgroko.backend.modelo;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "valor_caracteristica")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ValorCaracteristica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_valor")
    private Long idValor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_caracteristica", nullable = false)
    private CaracteristicaTecnica caracteristicaTecnica;

    @Column(name = "codigo", nullable = false, length = 50)
    private String codigo;

    @Column(name = "color_hex", length = 7)
    private String colorHex;
}