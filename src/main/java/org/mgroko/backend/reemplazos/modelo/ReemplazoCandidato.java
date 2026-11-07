package org.mgroko.backend.reemplazos.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.mgroko.backend.modelo.Perfil;
import org.mgroko.backend.modelo.Usuario;
import org.mgroko.backend.reemplazos.enums.EstadoCandidatoReemplazo;
import org.mgroko.backend.reemplazos.enums.NivelUbicacion;
import org.mgroko.backend.reemplazos.enums.TipoDisponibilidad;

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
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Candidato propuesto para un proceso de reemplazo.
 *
 * <p>La misma fila funciona como propuesta, invitación y registro de la
 * decisión final. El estado indica en qué etapa está.</p>
 */
@Entity
@Table(name = "reemplazo_candidato")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReemplazoCandidato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_candidato")
    private Long idCandidato;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_reemplazo", nullable = false)
    private Reemplazo reemplazo;

    @Column(name = "id_proyecto", nullable = false)
    private Long idProyecto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_perfil", nullable = false)
    private Perfil perfil;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 40)
    private EstadoCandidatoReemplazo estado;

    @Column(name = "orden_ranking", nullable = false)
    private Integer ordenRanking;

    @Column(name = "puntaje", nullable = false, precision = 6, scale = 2)
    private BigDecimal puntaje;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_ubicacion", nullable = false, length = 20)
    private NivelUbicacion nivelUbicacion;

    @Column(name = "es_alternativo", nullable = false)
    private Boolean esAlternativo;

    @Column(name = "detalle_faltantes", length = 1000)
    private String detalleFaltantes;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_disponibilidad", nullable = false, length = 30)
    private TipoDisponibilidad tipoDisponibilidad;

    @Column(name = "inicio_propuesto")
    private LocalDateTime inicioPropuesto;

    @Column(name = "fin_propuesto")
    private LocalDateTime finPropuesto;

    @Column(name = "mensaje", length = 200)
    private String mensaje;

    @Column(name = "fecha_invitacion")
    private LocalDateTime fechaInvitacion;

    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;

    @Column(name = "motivo_respuesta", length = 200)
    private String motivoRespuesta;

    @Column(name = "fecha_decision")
    private LocalDateTime fechaDecision;

    @Column(name = "id_usuario_decision")
    private Long idUsuarioDecision;

    @Column(name = "motivo_decision", length = 200)
    private String motivoDecision;

    @Column(name = "id_miembro_creado")
    private Long idMiembroCreado;

    @Column(name = "id_asignacion_creada")
    private Long idAsignacionCreada;

    /**
     * Versión de los términos y condiciones aceptada al aprobar la
     * incorporación. Se completa en memoria para la respuesta; la fuente de
     * verdad es {@code miembros_proyecto.id_tyc_aceptado}.
     */
    @Transient
    private Long idTycAceptado;
}
