package org.mgroko.backend.modelo;

import org.mgroko.backend.modelo.enums.EstadoSolicitud;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "invitacion_gral")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InvitacionGral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_invitacion_gral")
    private Long idInvitacionGral;

    @Column(name = "mensaje", length = 200)
    private String mensaje;

    @Column(name = "fecha_envio", nullable = false)
    @Builder.Default
    private LocalDateTime fechaEnvio = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoSolicitud estado = EstadoSolicitud.Pendiente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_perfil_remitente", nullable = false)
    private Perfil perfilRemitente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_perfil_destinatario", nullable = false)
    private Perfil perfilDestinatario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proyecto", nullable = false)
    private Proyecto proyecto;

    /**
     * Queda opcional: una invitación general al proyecto no selecciona un
     * requerimiento específico. Se conserva para invitaciones asociadas a un
     * requerimiento general o para registros históricos.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "id_requerimiento_gral")
    private RequerimientoGralProyecto requerimientoGral;
}
