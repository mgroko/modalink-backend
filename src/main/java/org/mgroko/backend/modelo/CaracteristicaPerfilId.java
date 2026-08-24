package org.mgroko.backend.modelo;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CaracteristicaPerfilId implements Serializable {

    private Long idPerfil;

    private Long idCaracteristica;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CaracteristicaPerfilId that)) return false;
        return Objects.equals(idPerfil, that.idPerfil)
                && Objects.equals(idCaracteristica, that.idCaracteristica);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idPerfil, idCaracteristica);
    }
}