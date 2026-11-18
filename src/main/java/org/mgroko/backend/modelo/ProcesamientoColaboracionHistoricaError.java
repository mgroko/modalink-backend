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
 * Error individual de un procesamiento histórico. No aborta el job.
 */
@Entity
@Table(name = "procesamiento_colaboracion_historica_error")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcesamientoColaboracionHistoricaError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_error")
    private Long idError;

    @Column(name = "id_procesamiento", nullable = false)
    private Long idProcesamiento;

    @Column(name = "id_publicacion")
    private Long idPublicacion;

    @Column(name = "id_imagen")
    private Long idImagen;

    @Column(name = "nombre_archivo", length = 255)
    private String nombreArchivo;

    @Column(name = "mensaje", nullable = false, length = 1000)
    private String mensaje;

    @Column(name = "fecha_registro", nullable = false)
    @Builder.Default
    private LocalDateTime fechaRegistro = LocalDateTime.now();
}
