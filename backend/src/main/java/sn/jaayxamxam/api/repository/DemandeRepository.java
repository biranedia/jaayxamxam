package sn.jaayxamxam.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.jaayxamxam.api.domain.Demande;
import sn.jaayxamxam.api.domain.StatutDemande;

import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;

public interface DemandeRepository extends JpaRepository<Demande, Long> {

    Optional<Demande> findByCodeSuivi(String codeSuivi);

    // Les demandes adressées à un artisan donné, la plus récente en premier :
    // c'est ce qu'il voit en ouvrant son espace.
    List<Demande> findByArtisanIdOrderByCreeLeDesc(Long artisanId);

    // Vue d'ensemble du back-office
    List<Demande> findTop100ByOrderByCreeLeDesc();

    // Les demandes que personne n'a encore prises : le vrai signal d'alerte de la plateforme
    long countByStatutAndCreeLeBefore(StatutDemande statut, LocalDateTime limite);

    long countByStatut(StatutDemande statut);
}
