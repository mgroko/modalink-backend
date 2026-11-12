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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Token de un solo uso para auto-login desde un email de notificación.
 *
 * <p>El valor crudo nunca se persiste: solo se guarda su hash SHA-256. El token
 * se vincula a una entrega de email y al usuario destinatario. Si la entrega o
 * la notificación se eliminan, el token se elimina por cascada.</p>
 */
@Entity
@Table(name = "token_acceso_email")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenAccesoEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_token")
    private Long idToken;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_entrega", nullable = false, unique = true)
    private NotificacionEntrega entrega;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_perfil_contexto")
    private Perfil perfilContexto;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;

    @Column(name = "usado_en")
    private LocalDateTime usadoEn;
}
