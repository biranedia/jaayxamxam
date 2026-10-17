package sn.jaayxamxam.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.jaayxamxam.api.domain.Course;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByCodeSuivi(String codeSuivi);
}
