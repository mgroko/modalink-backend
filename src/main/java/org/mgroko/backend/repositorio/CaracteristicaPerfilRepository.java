package org.mgroko.backend.repositorio;

import java.util.Collection;
import java.util.List;

import org.mgroko.backend.modelo.CaracteristicaPerfil;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaracteristicaPerfilRepository extends JpaRepository<CaracteristicaPerfil, Long> {

    boolean existsByCaracteristicaTecnicaIdCaracteristica(Long idCaracteristica);

    boolean existsByValorCaracteristica_Id_IdValor(Long idValor);

    @EntityGraph(attributePaths = {
            "caracteristicaTecnica",
            "caracteristicaTecnica.unidadMedida",
            "valorCaracteristica"
    })
    List<CaracteristicaPerfil> findByPerfil_IdPerfilIn(Collection<Long> idsPerfil);
}