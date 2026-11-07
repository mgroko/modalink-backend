package org.mgroko.backend.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Catálogo cerrado de eventos críticos de auditoría (UC-67).
 */
@Entity
@Table(name = "tipo_evento_auditoria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoEventoAuditoria {

    @Id
    @Column(name = "codigo", nullable = false, length = 60)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "modulo", nullable = false, length = 40)
    private String modulo;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;
}
