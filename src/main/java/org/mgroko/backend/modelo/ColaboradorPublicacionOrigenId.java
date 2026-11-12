package org.mgroko.backend.modelo;

import java.io.Serializable;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ColaboradorPublicacionOrigenId implements Serializable {

    private Long idPublicacion;
    private Long idPerfil;

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof ColaboradorPublicacionOrigenId that)) {
            return false;
        }
        return Objects.equals(idPublicacion, that.idPublicacion)
                && Objects.equals(idPerfil, that.idPerfil);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idPublicacion, idPerfil);
    }
}
