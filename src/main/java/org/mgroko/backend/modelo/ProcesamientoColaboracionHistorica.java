package org.mgroko.backend.modelo;

import java.time.LocalDateTime;

import org.mgroko.backend.modelo.enums.EstadoProcesamientoHistorico;
import org.mgroko.backend.modelo.enums.ModoProcesamientoHistorico;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Job administrativo que procesa publicaciones históricas para completar
 * huellas y detectar colaboraciones automáticas.
 */
@Entity
@Table(name = "procesamiento_colaboracion_historica")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcesamientoColaboracionHistorica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_procesamiento")
    private Long idProcesamiento;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoProcesamientoHistorico estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo", nullable = false, length = 20)
    private ModoProcesamientoHistorico modo;

    @Column(name = "filtro_id_publicacion")
    private Long filtroIdPublicacion;

    @Column(name = "filtro_id_perfil")
    private Long filtroIdPerfil;

    @Column(name = "filtro_fecha_desde")
    private LocalDateTime filtroFechaDesde;

    @Column(name = "filtro_fecha_hasta")
    private LocalDateTime filtroFechaHasta;

    @Column(name = "notificar", nullable = false)
    @Builder.Default
    private boolean notificar = false;

    @Column(name = "tamano_lote", nullable = false)
    @Builder.Default
    private int tamanoLote = 50;

    @Column(name = "limite_publicaciones")
    private Integer limitePublicaciones;

    @Column(name = "total_publicaciones", nullable = false)
    @Builder.Default
    private int totalPublicaciones = 0;

    @Column(name = "publicaciones_procesadas", nullable = false)
    @Builder.Default
    private int publicacionesProcesadas = 0;

    @Column(name = "total_imagenes", nullable = false)
    @Builder.Default
    private int totalImagenes = 0;

    @Column(name = "imagenes_procesadas", nullable = false)
    @Builder.Default
    private int imagenesProcesadas = 0;

    @Column(name = "hashes_calculados", nullable = false)
    @Builder.Default
    private int hashesCalculados = 0;

    @Column(name = "propuestas_creadas", nullable = false)
    @Builder.Default
    private int propuestasCreadas = 0;

    @Column(name = "propuestas_omitidas", nullable = false)
    @Builder.Default
    private int propuestasOmitidas = 0;

    @Column(name = "errores", nullable = false)
    @Builder.Default
    private int errores = 0;

    @Column(name = "ultimo_id_publicacion")
    private Long ultimoIdPublicacion;

    @Column(name = "id_usuario_admin", nullable = false)
    private Long idUsuarioAdmin;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "mensaje_error", length = 1000)
    private String mensajeError;
}
