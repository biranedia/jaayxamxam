package sn.jaayxamxam.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.jaayxamxam.api.domain.Avis;

import java.util.Collection;
import java.util.List;

public interface AvisRepository extends JpaRepository<Avis, Long> {

    boolean existsByDemandeId(Long demandeId);

    boolean existsByCourseId(Long courseId);

    // Avis visibles d'un artisan, les plus récents d'abord
    List<Avis> findByArtisanIdAndMasqueFalseOrderByCreeLeDesc(Long artisanId);

    // Note moyenne (null s'il n'y a aucun avis visible)
    @Query("select avg(a.note) from Avis a where a.artisan.id = :artisanId and a.masque = false")
    Double moyenne(@Param("artisanId") Long artisanId);

    // Moyenne et nombre d'avis pour plusieurs artisans en une seule requête
    @Query("""
            select a.artisan.id, avg(a.note), count(a) from Avis a
            where a.masque = false and a.artisan.id in :ids
            group by a.artisan.id
            """)
    List<Object[]> statsArtisans(@Param("ids") Collection<Long> ids);

    @Query("""
            select a.livreur.id, avg(a.note), count(a) from Avis a
            where a.masque = false and a.livreur.id in :ids
            group by a.livreur.id
            """)
    List<Object[]> statsLivreurs(@Param("ids") Collection<Long> ids);
}
