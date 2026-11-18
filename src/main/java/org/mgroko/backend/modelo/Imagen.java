package org.mgroko.backend.modelo;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "imagen")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Imagen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_imagen")
    private Long idImagen;

    @Column(name = "url", nullable = false, length = 1024)
    private String url;

    @Column(name = "estado", length = 20)
    private String estado;

    @Column(name = "nombre_archivo", nullable = false, length = 150)
    private String nombreArchivo;

    @Column(name = "tipo_imagen", nullable = false, length = 20)
    private String tipoImagen;

    @Column(name = "tamano_bytes", nullable = false)
    private Integer tamanoBytes;

    @Column(name = "fecha_subida", nullable = false)
    @Builder.Default
    private LocalDateTime fechaSubida = LocalDateTime.now();

    /**
     * SHA-256 de los bytes originales subidos. Permite detectar el mismo
     * archivo aunque cambie la configuración de procesamiento.
     */
    @Column(name = "sha256_original", length = 64)
    private String sha256Original;

    /**
     * SHA-256 del WebP procesado y almacenado.
     */
    @Column(name = "sha256_contenido", length = 64)
    private String sha256Contenido;

    /**
     * pHash OpenCV de 64 bits. Se guarda como 8 bytes.
     */
    @Column(name = "phash")
    private Long phash;

    /**
     * Versión del algoritmo de pHash para invalidar hashes incompatibles.
     */
    @Column(name = "phash_version")
    private Short phashVersion;
}
