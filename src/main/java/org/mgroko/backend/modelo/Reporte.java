package org.mgroko.backend.modelo;

import java.time.LocalDateTime;

import org.mgroko.backend.modelo.enums.EntidadReporte;
import org.mgroko.backend.modelo.enums.EstadoReporte;
import org.mgroko.backend.modelo.enums.MotivoReporte;

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
 * UC-15: reporte emitido por un perfil contra otra entidad de la plataforma.
 *
 * <p>El modelo es genérico ({@code entidadTipo + entidadId}) para reutilizarlo
 * en UC-69. Para reportes de perfil se completa además
 * {@link #perfilReportado}, que mantiene la FK real con {@code perfil}.</p>
 */
@Entity
@Table(name = "reporte")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Reporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reporte")
    private Long idReporte;

    @Enumerated(EnumType.STRING)
    @Column(name = "entidad_tipo", nullable = false, length = 20)
    private EntidadReporte entidadTipo;

    @Column(name = "entidad_id", nullable = false)
    private Long entidadId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario_autor", nullable = false)
    private Usuario usuarioAutor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_perfil_autor", nullable = false)
    private Perfil perfilAutor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_perfil_reportado")
    private Perfil perfilReportado;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo", nullable = false, length = 30)
    private MotivoReporte motivo;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoReporte estado = EstadoReporte.PENDIENTE;

    @Column(name = "fecha_reporte", nullable = false)
    private LocalDateTime fechaReporte;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_admin_revisor")
    private Usuario adminRevisor;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;
}
