package org.mgroko.backend.repositorio;

import java.util.List;
import java.util.Set;

import org.mgroko.backend.modelo.CaracteristicaTecnica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CaracteristicaTecnicaRepository extends JpaRepository<CaracteristicaTecnica, Long> {

    /**
     * Busca características técnicas que coincidan con los filtros indicados.
     * El filtro por profesión es opcional (null = sin filtro) y se combina con AND.
     * La búsqueda por texto no distingue mayúsculas: {@code patronCodigo} y
     * {@code patronUnidad} deben ser patrones {@code LIKE} ya formados y en
     * minúsculas con los caracteres especiales escapados (ver
     * {@code LikePatrones}); usar {@code "%"} para no filtrar.
     *
     * El join con {@code unidadMedida} debe ser LEFT: por diseño las características
     * ENUMERADO tienen {@code id_unidad} NULL, y un JOIN implícito se renderiza como
     * INNER y las excluiría del resultado incluso sin filtro de unidad.
     *
     * @param patronCodigo  patrón LIKE en minúsculas para el código (o "%")
     * @param patronUnidad  patrón LIKE en minúsculas para la unidad (o "%")
     * @param idProfesion   id de la profesión asociada (o null)
     * @return características técnicas que coinciden con los criterios
     */
    @Query("""
            SELECT c FROM CaracteristicaTecnica c
            LEFT JOIN c.unidadMedida um
            WHERE (:patronCodigo = '%' OR c.codigo ILIKE :patronCodigo ESCAPE '\\')
              AND (:patronUnidad = '%' OR (um IS NOT NULL AND um.simbolo ILIKE :patronUnidad ESCAPE '\\'))
              AND (:idProfesion IS NULL OR c.profesion.idProfesion = :idProfesion)
            ORDER BY c.codigo
            """)
    List<CaracteristicaTecnica> buscar(@Param("patronCodigo") String patronCodigo,
                                       @Param("patronUnidad") String patronUnidad,
                                       @Param("idProfesion") Long idProfesion);

    List<CaracteristicaTecnica> findAllByOrderByCodigo();

    boolean existsByCodigo(String codigo);

    boolean existsByUnidadMedidaIdUnidad(Long idUnidad);

    List<CaracteristicaTecnica> findAllByUnidadMedidaIdUnidad(Long idUnidad);

    @Query("""
            SELECT true FROM CaracteristicaTecnica c
            WHERE UPPER(c.codigo) = UPPER(:codigo)
            """)
    boolean existsByCodigoIgnoreCase(@Param("codigo") String codigo);

    /**
     * Indica si la característica está "en uso": tiene valores cargados en
     * perfiles o está referenciada por requerimientos de actividad/proyecto.
     *
     * Es la misma regla que aplica el trigger
     * {@code trg_caracteristica_en_uso_solo_nombre} en la base de datos:
     * cuando está en uso, sólo puede modificarse el nombre.
     *
     * Se consulta nativamente porque {@code requerimiento_act_caract} y
     * {@code requerimiento_gral_caract} están mapeadas como tablas de unión
     * {@code @ManyToMany} sin sus columnas de rango.
     *
     * @param idCaracteristica id de la característica técnica
     * @return true si alguna fila la referencia
     */
    @Query(value = """
            SELECT EXISTS (SELECT 1 FROM caracteristica_perfil WHERE id_caracteristica = :id)
                OR EXISTS (SELECT 1 FROM requerimiento_act_caract WHERE id_caracteristica = :id)
                OR EXISTS (SELECT 1 FROM requerimiento_gral_caract WHERE id_caracteristica = :id)
            """, nativeQuery = true)
    boolean existeEnUso(@Param("id") Long idCaracteristica);

    /**
     * Devuelve el conjunto de ids de características que están en uso
     * (ver {@link #existeEnUso(Long)}), para poblar el flag {@code enUso}
     * de la respuesta de listado con una sola consulta por tabla.
     *
     * @return ids de características en uso (vacío si ninguna lo está)
     */
    @Query(value = """
            SELECT id_caracteristica FROM caracteristica_perfil
            UNION
            SELECT id_caracteristica FROM requerimiento_act_caract
            UNION
            SELECT id_caracteristica FROM requerimiento_gral_caract
            """, nativeQuery = true)
    Set<Long> findIdsEnUso();
}