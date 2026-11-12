package az.baku.managesystemback.repository;

import az.baku.managesystemback.entity.Site;
import az.baku.managesystemback.entity.SiteStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * @author Fatima Sultanova
 */
public interface SiteRepository extends JpaRepository<Site, Long> {

    @EntityGraph(attributePaths = "template")
    @Query("""
            select s from Site s
            where s.owner.id = :ownerId
              and (:status is null or s.status = :status)
              and (:search is null
                   or lower(s.projectName) like lower(concat('%', :search, '%'))
                   or lower(s.slug) like lower(concat('%', :search, '%')))
            order by s.createdAt desc
            """)
    List<Site> findAllForOwner(
            @Param("ownerId") long ownerId,
            @Param("status") SiteStatus status,
            @Param("search") String search
    );

    @EntityGraph(attributePaths = "template")
    Optional<Site> findByIdAndOwnerId(long id, long ownerId);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, long id);

    long countByOwnerId(long ownerId);
}
