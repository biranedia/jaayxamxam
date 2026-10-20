package sn.jaayxamxam.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.jaayxamxam.api.domain.Course;
import sn.jaayxamxam.api.domain.StatutCourse;

import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByCodeSuivi(String codeSuivi);

    // Les courses confiées à un livreur donné, la plus récente en premier.
    List<Course> findByLivreurIdOrderByCreeLeDesc(Long livreurId);

    List<Course> findTop100ByOrderByCreeLeDesc();

    long countByStatutAndCreeLeBefore(StatutCourse statut, LocalDateTime limite);

    long countByStatut(StatutCourse statut);
}
