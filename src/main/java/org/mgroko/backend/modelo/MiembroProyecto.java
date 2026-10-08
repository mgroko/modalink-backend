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
    @Column(name = "estado_participacion", nullable = false, length = 50)
    @Builder.Default
    private EstadoParticipacion estadoParticipacion = EstadoParticipacion.ACTIVO;

    @Column(name = "\"motivoBaja\"", length = 100)
    private String motivoBaja;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proyecto", nullable = false)
    private Proyecto proyecto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_perfil", nullable = false)
    private Perfil perfil;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_rol_proyecto", nullable = false)
    private RolProyecto rolProyecto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "id_perfil", referencedColumnName = "id_perfil", insertable = false, updatable = false),
        @JoinColumn(name = "id_tyc_aceptado", referencedColumnName = "id_tyc", insertable = false, updatable = false)
    })
    private PerfilTyc tycAceptado;

    @Column(name = "id_tyc_aceptado")
    @Builder.Default
    private Long idTycAceptado = 1L;
}
