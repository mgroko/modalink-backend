package org.mgroko.backend.modelo;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "perfil_tyc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PerfilTyc {

    @EmbeddedId
    private PerfilTycId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idPerfil")
    @JoinColumn(name = "id_perfil")
    private Perfil perfil;

    /**
     * Número ordinal de la versión dentro del perfil. La base lo exige NOT
     * NULL; se deja nullable en el mapeo para que el trigger
     * {@code trg_asignar_numero_version_tyc} complete las inserciones que no
     * lo informen (tests, importaciones).
     */
    @Column(name = "numero_version")
    private Integer numeroVersion;

    @Column(name = "descripcion", nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "fecha_inicio", nullable = false)
    @Builder.Default
    private LocalDateTime fechaInicio = LocalDateTime.now();

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    public Long getIdTyc() {
        return id != null ? id.getIdTyc() : null;
    }
}
