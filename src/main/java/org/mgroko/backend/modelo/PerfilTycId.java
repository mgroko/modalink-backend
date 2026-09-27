package org.mgroko.backend.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PerfilTycId implements Serializable {

    @Column(name = "id_perfil")
    private Long idPerfil;

    @Column(name = "id_tyc")
    private Long idTyc;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PerfilTycId that = (PerfilTycId) o;
        return Objects.equals(idPerfil, that.idPerfil) && Objects.equals(idTyc, that.idTyc);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idPerfil, idTyc);
    }
}
