package org.mgroko.backend.modelo;

import java.io.Serializable;
import java.util.Objects;

public class RequerimientoGralCaractValorId implements Serializable {

    private Long idValor;
    private Long idCaracteristica;
    private Long idReqGralCaract;

    public RequerimientoGralCaractValorId() {}

    public RequerimientoGralCaractValorId(Long idValor, Long idCaracteristica, Long idReqGralCaract) {
        this.idValor = idValor;
        this.idCaracteristica = idCaracteristica;
        this.idReqGralCaract = idReqGralCaract;
    }

    public Long getIdValor() {
        return idValor;
    }

    public Long getIdCaracteristica() {
        return idCaracteristica;
    }

    public Long getIdReqGralCaract() {
        return idReqGralCaract;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof RequerimientoGralCaractValorId that))
            return false;
        return Objects.equals(idValor, that.idValor)
                && Objects.equals(idCaracteristica, that.idCaracteristica)
                && Objects.equals(idReqGralCaract, that.idReqGralCaract);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idValor, idCaracteristica, idReqGralCaract);
    }
}
