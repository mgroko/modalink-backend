package org.mgroko.program.modelo;

import org.mgroko.program.modelo.enums.EstadoSolicitud;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "postulacion_gral")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PostulacionGral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_postulacion_gral")
    private Long idPostulacionGral;

    @Column(name = "fecha_postulacion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaPostulacion = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoSolicitud estado = EstadoSolicitud.Pendiente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_perfil", nullable = false)
    private Perfil perfil;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_requerimiento_gral", nullable = false)
    private RequerimientoGralProyecto requerimientoGral;
}
