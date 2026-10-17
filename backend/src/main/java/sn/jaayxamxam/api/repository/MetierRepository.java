package sn.jaayxamxam.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.jaayxamxam.api.domain.Metier;

import java.util.List;

public interface MetierRepository extends JpaRepository<Metier, Long> {

    // Seuls les métiers actifs sont proposés à la recherche, rangés par famille puis par nom
    List<Metier> findByActifTrueOrderByFamilleAscNomAsc();
}
