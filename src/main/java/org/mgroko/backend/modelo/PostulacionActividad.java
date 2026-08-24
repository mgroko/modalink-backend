package org.mgroko.backend.modelo;

import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "postulacion_actividad")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PostulacionActividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_postulacion")
    private Long idPostulacion;

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
    @JoinColumn(name = "id_requerimiento", nullable = false)
    private RequerimientoActividad requerimiento;
}
