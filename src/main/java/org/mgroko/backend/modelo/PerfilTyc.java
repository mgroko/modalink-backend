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

    @Column(name = "descripcion", nullable = false, length = 200)
    private String descripcion;

    @Column(name = "fecha_inicio", nullable = false)
    @Builder.Default
    private LocalDateTime fechaInicio = LocalDateTime.now();

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;
}
