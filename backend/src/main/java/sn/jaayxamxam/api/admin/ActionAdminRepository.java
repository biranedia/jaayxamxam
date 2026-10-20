package sn.jaayxamxam.api.admin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActionAdminRepository extends JpaRepository<ActionAdmin, Long> {

    /** Les 200 dernières actions, la plus récente en premier : ce que montre le back-office. */
    List<ActionAdmin> findTop200ByOrderByFaiteLeDesc();
}
