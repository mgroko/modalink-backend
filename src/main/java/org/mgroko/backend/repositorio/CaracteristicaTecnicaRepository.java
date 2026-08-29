package org.mgroko.backend.repositorio;

import java.util.List;

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
     * minúsculas (ej. {@code "%altura%"}); usar {@code "%"} para no filtrar.
     *
     * @param patronCodigo  patrón LIKE en minúsculas para el código (o "%")
     * @param patronUnidad  patrón LIKE en minúsculas para la unidad (o "%")
     * @param idProfesion   id de la profesión asociada (o null)
     * @return características técnicas que coinciden con los criterios
     */
    @Query("""
            SELECT c FROM CaracteristicaTecnica c
            WHERE (:patronCodigo = '%' OR LOWER(c.codigo) LIKE :patronCodigo)
              AND (:patronUnidad = '%' OR (c.unidad IS NOT NULL AND LOWER(c.unidad) LIKE :patronUnidad))
              AND (:idProfesion IS NULL OR c.profesion.idProfesion = :idProfesion)
            ORDER BY c.codigo
            """)
    List<CaracteristicaTecnica> buscar(@Param("patronCodigo") String patronCodigo,
                                       @Param("patronUnidad") String patronUnidad,
                                       @Param("idProfesion") Long idProfesion);

    List<CaracteristicaTecnica> findAllByOrderByCodigo();

    boolean existsByCodigo(String codigo);
}