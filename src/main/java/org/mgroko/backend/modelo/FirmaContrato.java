package org.mgroko.backend.modelo;

import java.time.LocalDateTime;

import org.mgroko.backend.modelo.enums.EstadoFirmaContrato;
import org.mgroko.backend.modelo.enums.RolParteContrato;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Parte firmante de un contrato (UC-39/UC-55).
 *
 * <p>La PK es compuesta: (id_miembro, id_contrato). Las asociaciones
 * {@code miembro} y {@code contrato} son de solo lectura; las columnas FK se
 * escriben a traves de {@code idMiembro} e {@code idContrato}.</p>
 */
@Entity
@Table(name = "firma_contrato")
@IdClass(FirmaContratoId.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FirmaContrato {

    @Id
    @Column(name = "id_miembro", nullable = false)
    private Long idMiembro;

    @Id
    @Column(name = "id_contrato", nullable = false)
    private Long idContrato;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_miembro", insertable = false, updatable = false)
    private MiembroProyecto miembro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_contrato", insertable = false, updatable = false)
    private Contrato contrato;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_contrato_version")
    private ContratoVersion contratoVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol_parte", nullable = false, length = 30)
    private RolParteContrato rolParte;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_firma", nullable = false, length = 20)
    @Builder.Default
    private EstadoFirmaContrato estadoFirma = EstadoFirmaContrato.PENDIENTE;

    @Column(name = "fecha_firma")
    private LocalDateTime fechaFirma;

    @Column(name = "fecha_rechazo")
    private LocalDateTime fechaRechazo;

    @Column(name = "motivo_rechazo", length = 200)
    private String motivoRechazo;
}
