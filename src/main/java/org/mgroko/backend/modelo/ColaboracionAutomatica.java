package org.mgroko.backend.modelo;

import java.time.LocalDateTime;

import org.mgroko.backend.modelo.enums.EstadoColaboracionAutomatica;
import org.mgroko.backend.modelo.enums.EstadoConfirmacionColaboracion;

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
 * Propuesta de colaboración generada por coincidencia de imágenes entre dos
 * publicaciones independientes.
 *
 * <p>Cada publicación conserva su dueño. Al confirmar ambos dueños, cada uno
 * queda como colaborador de la publicación del otro. No se crea ninguna
 * conexión de red.</p>
 */
@Entity
@Table(name = "colaboracion_automatica")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColaboracionAutomatica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_colaboracion_automatica")
    private Long idColaboracionAutomatica;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_imagen")
    private Imagen imagen;

    @Column(name = "sha256_evidencia", length = 64)
    private String sha256Evidencia;

    @Column(name = "phash_version")
    private Short phashVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_publicacion_a")
    private Publicacion publicacionA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_publicacion_b")
    private Publicacion publicacionB;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_perfil_a", nullable = false)
    private Perfil perfilA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_perfil_b", nullable = false)
    private Perfil perfilB;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoColaboracionAutomatica estado = EstadoColaboracionAutomatica.PENDIENTE;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_perfil_a", nullable = false, length = 20)
    @Builder.Default
    private EstadoConfirmacionColaboracion estadoPerfilA =
            EstadoConfirmacionColaboracion.PENDIENTE;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_perfil_b", nullable = false, length = 20)
    @Builder.Default
    private EstadoConfirmacionColaboracion estadoPerfilB =
            EstadoConfirmacionColaboracion.PENDIENTE;

    @Column(name = "fecha_deteccion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaDeteccion = LocalDateTime.now();

    @Column(name = "fecha_respuesta_a")
    private LocalDateTime fechaRespuestaA;

    @Column(name = "fecha_respuesta_b")
    private LocalDateTime fechaRespuestaB;

    @Column(name = "fecha_confirmacion")
    private LocalDateTime fechaConfirmacion;

    @Column(name = "fecha_disolucion")
    private LocalDateTime fechaDisolucion;
}
