package org.mgroko.backend.repositorio;

import org.mgroko.backend.modelo.CaracteristicaPerfil;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaracteristicaPerfilRepository extends JpaRepository<CaracteristicaPerfil, Long> {

    boolean existsByCaracteristicaTecnicaIdCaracteristica(Long idCaracteristica);

    boolean existsByValorCaracteristica_Id_IdValor(Long idValor);
}