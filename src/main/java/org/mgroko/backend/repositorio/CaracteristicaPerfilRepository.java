package org.mgroko.backend.repositorio;

import org.mgroko.backend.modelo.CaracteristicaPerfil;
import org.mgroko.backend.modelo.CaracteristicaPerfilId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaracteristicaPerfilRepository extends JpaRepository<CaracteristicaPerfil, CaracteristicaPerfilId> {

    boolean existsByCaracteristicaTecnicaIdCaracteristica(Long idCaracteristica);

    boolean existsByValorCaracteristicaIdValor(Long idValor);
}