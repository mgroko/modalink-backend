package org.mgroko.backend.modelo;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Registro de auditoría generado por los triggers de BD.
 *
 * <p>{@code idUsuario} representa el operador real de la aplicación, no el
 * usuario afectado. La entidad afectada se identifica con
 * {@code entidadAfectada} + {@code idAfectado} (o {@code entidadClave} para
 * claves textuales como {@code configuracion_sistema}).</p>
 */
@Entity
@Table(name = "auditoria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Long idAuditoria;

    @Column(name = "usuario_bd", nullable = false, length = 200)
    private String usuarioBd;

    @Column(name = "entidad_afectada", nullable = false, length = 100)
    private String entidadAfectada;

    @Column(name = "entidad_clave", length = 100)
    private String entidadClave;

    @Column(name = "id_afectado", nullable = false)
    private Long idAfectado;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "ip_usuario", length = 45)
    private String ipUsuario;

    @Column(name = "id_tipo_auditoria", nullable = false)
    private Long idTipoAuditoria;

    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(name = "tipo_evento", length = 60)
    private String tipoEvento;

    @Column(name = "tipo_evento_original", length = 60)
    private String tipoEventoOriginal;
}
