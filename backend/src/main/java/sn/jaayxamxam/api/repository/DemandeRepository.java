package sn.jaayxamxam.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.jaayxamxam.api.domain.Demande;

import java.util.Optional;

public interface DemandeRepository extends JpaRepository<Demande, Long> {

    Optional<Demande> findByCodeSuivi(String codeSuivi);
}
