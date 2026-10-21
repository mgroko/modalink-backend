package org.mgroko.backend.modelo;

import java.io.Serializable;
import java.util.Objects;

public class RequerimientoActCaractValorId implements Serializable {

    private Long idReqActCaract;
    private Long idCaracteristica;
    private Long idValor;

    public RequerimientoActCaractValorId() {
    }

    public RequerimientoActCaractValorId(Long idReqActCaract, Long idCaracteristica, Long idValor) {
        this.idReqActCaract = idReqActCaract;
        this.idCaracteristica = idCaracteristica;
        this.idValor = idValor;
    }

    public Long getIdReqActCaract() {
        return idReqActCaract;
    }

    public Long getIdCaracteristica() {
        return idCaracteristica;
    }

    public Long getIdValor() {
        return idValor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RequerimientoActCaractValorId that)) {
            return false;
        }
        return Objects.equals(idReqActCaract, that.idReqActCaract)
                && Objects.equals(idCaracteristica, that.idCaracteristica)
                && Objects.equals(idValor, that.idValor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idReqActCaract, idCaracteristica, idValor);
    }
}
