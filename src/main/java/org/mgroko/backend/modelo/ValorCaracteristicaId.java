package org.mgroko.backend.modelo;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ValorCaracteristicaId implements Serializable {

    @Column(name = "id_valor")
    private Long idValor;

    @Column(name = "id_caracteristica")
    private Long idCaracteristica;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ValorCaracteristicaId that)) return false;
        return Objects.equals(idValor, that.idValor)
                && Objects.equals(idCaracteristica, that.idCaracteristica);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idValor, idCaracteristica);
    }
}
