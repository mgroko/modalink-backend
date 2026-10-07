package org.mgroko.backend.modelo;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "valor_caracteristica")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ValorCaracteristica {

    @EmbeddedId
    private ValorCaracteristicaId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("idCaracteristica")
    @JoinColumn(name = "id_caracteristica", nullable = false)
    private CaracteristicaTecnica caracteristicaTecnica;

    @Column(name = "etiqueta", length = 255)
    private String etiqueta;

    @Column(name = "color_hex", length = 7)
    private String colorHex;

    /**
     * Accessor de conveniencia para obtener idValor sin navegar por el EmbeddedId.
     */
    public Long getIdValor() {
        return id != null ? id.getIdValor() : null;
    }
}