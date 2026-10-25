package org.mgroko.backend.repositorio;

import java.time.LocalDateTime;
import java.util.List;

import org.mgroko.backend.modelo.Imagen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImagenRepository extends JpaRepository<Imagen, Long> {

    /**
     * Todos los nombres de archivo de la tabla imagen, para detectar qué
     * archivos del directorio tienen fila y cuáles están sueltos.
     *
     * @return nombres de archivo registrados
     */
    @Query("select i.nombreArchivo from Imagen i")
    List<String> listarNombresArchivos();

    /**
     * Imágenes sin ninguna referencia en el esquema (mapeo de la Fase 0:
     * perfil.foto_perfil, imagen_moodboard, imagen_publicacion y
     * polaroid_perfil) y cuya fecha de subida es anterior al límite pasado.
     * <p>
     * Consulta nativa porque dos de esas tablas aún no tienen entidad JPA.
     *
     * @param limite fecha límite de antigüedad (inclusive hacia atrás)
     * @return imágenes huérfanas
     */
    @Query(value = """
            SELECT i.* FROM imagen i
            WHERE i.fecha_subida < :limite
              AND NOT EXISTS (SELECT 1 FROM perfil p WHERE p.foto_perfil = i.id_imagen)
              AND NOT EXISTS (SELECT 1 FROM imagen_moodboard m WHERE m.id_imagen = i.id_imagen)
              AND NOT EXISTS (SELECT 1 FROM imagen_publicacion pu WHERE pu.id_imagen = i.id_imagen)
              AND NOT EXISTS (SELECT 1 FROM polaroid_perfil po WHERE po.id_imagen = i.id_imagen)
            """, nativeQuery = true)
    List<Imagen> buscarHuerfanas(@Param("limite") LocalDateTime limite);
}
