package org.mgroko.backend.modelo;

import java.math.BigDecimal;

import jakarta.persistence.*;

import lombok.*;

@Entity
@Table(name = "requerimiento_gral_caract")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RequerimientoGralCaract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_req_gral_caract")
    private Long idReqGralCaract;

    @Column(name = "valor_min")
    private BigDecimal valorMin;

    @Column(name = "valor_max")
    private BigDecimal valorMax;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_requerimiento_gral", nullable = false)
    private RequerimientoGralProyecto requerimientoGral;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_caracteristica", nullable = false)
    private CaracteristicaTecnica caracteristica;
}
