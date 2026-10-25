package org.mgroko.backend.modelo;

import jakarta.persistence.*;

import lombok.*;

@Entity
@Table(name = "requerimiento_gral_caract_valor")
@IdClass(RequerimientoGralCaractValorId.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RequerimientoGralCaractValor {

    @Id
    @Column(name = "id_valor")
    private Long idValor;

    @Id
    @Column(name = "id_caracteristica")
    private Long idCaracteristica;

    @Id
    @Column(name = "id_req_gral_caract")
    private Long idReqGralCaract;
}
