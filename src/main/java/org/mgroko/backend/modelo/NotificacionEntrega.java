package org.mgroko.backend.modelo;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Entrega de una notificación por un canal externo.
 *
 * <p>La tabla {@code notificacion_entrega} funciona como outbox: un trigger de
 * PostgreSQL encola una fila {@code PENDIENTE} por cada notificación de
 * prioridad ALTA. Un scheduler toma las filas pendientes, envía el email y
 * actualiza el estado. Si el envío falla, se reintenta con backoff.</p>
 */
@Entity
@Table(name = "notificacion_entrega")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificacionEntrega {

    public static final String CANAL_EMAIL = "EMAIL";
    public static final String ESTADO_PENDIENTE = "PENDIENTE";
    public static final String ESTADO_ENVIANDO = "ENVIANDO";
    public static final String ESTADO_ENVIADO = "ENVIADO";
    public static final String ESTADO_FALLIDO = "FALLIDO";
    public static final String ESTADO_CANCELADO = "CANCELADO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_entrega")
    private Long idEntrega;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_notificacion", nullable = false)
    private Notificacion notificacion;

    @Column(name = "canal", nullable = false, length = 20)
    @Builder.Default
    private String canal = CANAL_EMAIL;

    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private String estado = ESTADO_PENDIENTE;

    @Column(name = "intentos", nullable = false)
    @Builder.Default
    private Integer intentos = 0;

    @Column(name = "proximo_intento")
    private LocalDateTime proximoIntento;

    @Column(name = "fecha_ultimo_intento")
    private LocalDateTime fechaUltimoIntento;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    @Column(name = "error", length = 500)
    private String error;
}
