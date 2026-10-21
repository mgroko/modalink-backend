package org.mgroko.backend.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "requerimiento_act_caract_valor")
@IdClass(RequerimientoActCaractValorId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequerimientoActCaractValor {

    @Id
    @Column(name = "id_req_act_caract")
    private Long idReqActCaract;

    @Id
    @Column(name = "id_caracteristica")
    private Long idCaracteristica;

    @Id
    @Column(name = "id_valor")
    private Long idValor;
}
