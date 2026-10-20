package sn.jaayxamxam.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.jaayxamxam.api.domain.Artisan;

import java.util.List;
import java.util.Optional;

public interface ArtisanRepository extends JpaRepository<Artisan, Long> {

    boolean existsByTelephone(String telephone);

    // Le code de gestion prouve qu'on est le propriétaire du profil
    Optional<Artisan> findByCodeGestion(String codeGestion);

    // File d'attente de l'administration : ni validés, ni refusés
    List<Artisan> findByActifFalseAndRefuseFalseOrderByCreeLeAsc();

    long countByActifTrue();

    long countByRefuseTrue();

    // Recherche : les filtres métier et zone sont optionnels ; les artisans vérifiés passent en premier
    @Query("""
            select a from Artisan a join fetch a.metier join fetch a.zone
            where a.actif = true
              and (:metierId is null or a.metier.id = :metierId)
              and (:zoneId is null or a.zone.id = :zoneId)
            order by a.verifie desc, a.anneesExperience desc
            """)
    List<Artisan> rechercher(@Param("metierId") Long metierId, @Param("zoneId") Long zoneId);
}
