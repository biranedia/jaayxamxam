package sn.jaayxamxam.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.jaayxamxam.api.domain.Livreur;

import java.util.List;
import java.util.Optional;

public interface LivreurRepository extends JpaRepository<Livreur, Long> {

    boolean existsByTelephone(String telephone);

    // Le code de gestion prouve qu'on est le propriétaire du profil
    Optional<Livreur> findByCodeGestion(String codeGestion);

    // File d'attente de l'administration : ni validés, ni refusés
    List<Livreur> findByActifFalseAndRefuseFalseOrderByCreeLeAsc();

    long countByActifTrue();

    // Le filtre véhicule est facultatif ; les livreurs vérifiés passent en premier
    @Query("""
            select l from Livreur l
            where l.actif = true
              and (:vehicule is null or l.vehicule = :vehicule)
            order by l.verifie desc, l.derniereActivite desc nulls last
            """)
    List<Livreur> rechercher(@Param("vehicule") String vehicule);
}
