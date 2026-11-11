package org.mgroko.backend.modelo;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * UC-66: interacción "Me gusta" de un perfil sobre una publicación.
 *
 * <p>La tabla {@code me_gusta} tiene clave primaria compuesta, por lo que un
 * perfil no puede registrar más de un Me gusta sobre la misma publicación.</p>
 */
@Entity
@Table(name = "me_gusta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeGusta {

    @EmbeddedId
    private MeGustaId id;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;
}
