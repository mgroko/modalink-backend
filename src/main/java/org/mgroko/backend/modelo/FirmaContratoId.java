package org.mgroko.backend.modelo;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Clave compuesta de firma_contrato: (id_miembro, id_contrato).
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class FirmaContratoId implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long idMiembro;

    private Long idContrato;
}
