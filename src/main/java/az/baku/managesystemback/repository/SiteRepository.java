package az.baku.managesystemback.repository;

import az.baku.managesystemback.entity.Site;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * @author Fatima Sultanova
 */
public interface SiteRepository extends JpaRepository<Site, Long> {

    @EntityGraph(attributePaths = "template")
    List<Site> findAllByWorkspaceOwnerIdOrderByCreatedAtDesc(long ownerId);

    @EntityGraph(attributePaths = "template")
    Optional<Site> findByIdAndWorkspaceOwnerId(long id, long ownerId);

    boolean existsByWorkspaceIdAndSlug(long workspaceId, String slug);

    boolean existsByWorkspaceIdAndSlugAndIdNot(long workspaceId, String slug, long id);
}
