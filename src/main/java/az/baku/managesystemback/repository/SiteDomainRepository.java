package az.baku.managesystemback.repository;

import az.baku.managesystemback.entity.SiteDomain;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * @author Fatima Sultanova
 */
public interface SiteDomainRepository extends JpaRepository<SiteDomain, Long> {

    @EntityGraph(attributePaths = "site")
    Optional<SiteDomain> findBySiteId(long siteId);

    boolean existsByHostname(String hostname);

    @EntityGraph(attributePaths = "site")
    Optional<SiteDomain> findByHostname(String hostname);
}
