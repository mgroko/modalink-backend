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
 * Mensaje de texto de una conversación privada.
 *
 * <p>{@code leido} indica si la contraparte ya lo leyó. El emisor no cuenta
 * sus propios mensajes como no leídos.</p>
 */
@Entity
@Table(name = "mensaje")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Mensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mensaje")
    private Long idMensaje;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_conversacion", nullable = false)
    private Conversacion conversacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_perfil_emisor", nullable = false)
    private Perfil perfilEmisor;

    @Column(name = "contenido", nullable = false, length = 1000)
    private String contenido;

    @Column(name = "fecha_envio", nullable = false)
    @Builder.Default
    private LocalDateTime fechaEnvio = LocalDateTime.now();

    @Column(name = "leido", nullable = false)
    @Builder.Default
    private Boolean leido = false;

    @Column(name = "fecha_lectura")
    private LocalDateTime fechaLectura;
}
