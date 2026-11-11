package org.mgroko.backend.modelo;

import java.time.LocalDateTime;

import org.mgroko.backend.modelo.enums.EstadoSolicitud;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Solicitud de colaboración enviada por el dueño de una publicación a un
 * perfil de su red de contactos.
 *
 * <p>UC-61 crea la solicitud en estado {@code PENDIENTE}. UC-62/63 la pasan a
 * {@code ACEPTADA} o {@code RECHAZADA}. La aceptación agrega al perfil a
 * {@code colaborador_publicacion}; UC-61 no lo hace.</p>
 */
@Entity
@Table(name = "solicitud_colaboracion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudColaboracion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud_colaboracion")
    private Long idSolicitudColaboracion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_publicacion", nullable = false)
    private Publicacion publicacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_perfil_remitente", nullable = false)
    private Perfil perfilRemitente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_perfil_destinatario", nullable = false)
    private Perfil perfilDestinatario;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;

    @Column(name = "fecha_envio", nullable = false)
    @Builder.Default
    private LocalDateTime fechaEnvio = LocalDateTime.now();
}
