package org.mgroko.backend.modelo;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Clave compuesta de {@link MeGusta}: una publicación puede recibir un único
 * Me gusta por perfil.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MeGustaId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "id_publicacion", nullable = false)
    private Long idPublicacion;

    @Column(name = "id_perfil", nullable = false)
    private Long idPerfil;
}
