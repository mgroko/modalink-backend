package org.mgroko.backend.modelo;

import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicUpdate;
import org.mgroko.backend.modelo.enums.EstadoContrato;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Contrato asociado a un proyecto (UC-39).
 *
 * <p>La maquina de estados, las versiones, los eventos y las notificaciones
 * las gobiernan los triggers de la base de datos. Esta entidad solo persiste
 * los datos propios del contrato y su estado.</p>
 */
@Entity
@Table(name = "contrato")
@DynamicUpdate
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Contrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contrato")
    private Long idContrato;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proyecto", nullable = false)
    private Proyecto proyecto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tipo_acuerdo", nullable = false)
    private TipoAcuerdoContrato tipoAcuerdo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_miembro_emisor", nullable = false)
    private MiembroProyecto miembroEmisor;

    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    @Column(name = "texto", columnDefinition = "text")
    private String texto;

    @Column(name = "version_actual", nullable = false)
    @Builder.Default
    private Integer versionActual = 1;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_emision")
    private LocalDateTime fechaEmision;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @Column(name = "fecha_cancelacion")
    private LocalDateTime fechaCancelacion;

    @Column(name = "fecha_baja")
    private LocalDateTime fechaBaja;

    @Column(name = "motivo_cancelacion", length = 200)
    private String motivoCancelacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 50)
    @Builder.Default
    private EstadoContrato estado = EstadoContrato.BORRADOR;

    @PrePersist
    private void prepararFechaCreacion() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}
