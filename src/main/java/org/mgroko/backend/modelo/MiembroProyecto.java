package org.mgroko.backend.modelo;

import org.mgroko.backend.modelo.enums.EstadoParticipacion;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "miembros_proyecto")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MiembroProyecto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_miembro")
    private Long idMiembro;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_participacion", nullable = false, length = 30)
    @Builder.Default
    private EstadoParticipacion estadoParticipacion = EstadoParticipacion.Activo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proyecto", nullable = false)
    private Proyecto proyecto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_perfil", nullable = false)
    private Perfil perfil;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_rol_proyecto", nullable = false)
    private RolProyecto rolProyecto;

}
