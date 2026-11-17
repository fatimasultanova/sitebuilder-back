package az.baku.managesystemback.repository;

import az.baku.managesystemback.entity.Deployment;
import az.baku.managesystemback.entity.DeploymentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author Fatima Sultanova
 */
public interface DeploymentRepository extends JpaRepository<Deployment, Long> {
    long countByOwnerId(long ownerId);

    @EntityGraph(attributePaths = {"site", "site.template"})
    @Query("""
            select d from Deployment d
            where d.owner.id = :ownerId
              and (:siteId is null or d.site.id = :siteId)
              and (:status is null or d.status = :status)
            order by d.createdAt desc, d.id desc
            """)
    List<Deployment> findAllForOwner(
            @Param("ownerId") long ownerId,
            @Param("siteId") Long siteId,
            @Param("status") DeploymentStatus status
    );

    @EntityGraph(attributePaths = {"site", "site.template"})
    Optional<Deployment> findByIdAndOwnerId(long id, long ownerId);

    @EntityGraph(attributePaths = {"site", "site.template"})
    @Query("select d from Deployment d where d.id = :id")
    Optional<Deployment> findWithSiteAndTemplateById(@Param("id") long id);

    boolean existsBySiteIdAndStatusIn(long siteId, Collection<DeploymentStatus> statuses);

    boolean existsBySiteIdAndVersion(long siteId, Integer version);

    @Query("select coalesce(max(d.version), 0) from Deployment d where d.site.id = :siteId")
    int findMaxVersionBySiteId(@Param("siteId") long siteId);

    List<Deployment> findBySiteIdAndStatus(long siteId, DeploymentStatus status);
    List<Deployment> findBySiteIdAndStatusIn(long siteId, Collection<DeploymentStatus> statuses);

    @Query("select d.status from Deployment d where d.id = :id")
    Optional<DeploymentStatus> findStatusById(@Param("id") long id);

    @Query("""
            select d from Deployment d
            join fetch d.site s
            join fetch s.owner
            left join fetch s.template
            where s.slug = :slug
              and d.status = :status
            order by d.finishedAt desc nulls last, d.id desc
            """)
    List<Deployment> findLiveBySiteSlug(
            @Param("slug") String slug,
            @Param("status") DeploymentStatus status
    );

    @Modifying
    @Query("""
            update Deployment d
            set d.status = :failedStatus,
                d.logMessage = :message,
                d.finishedAt = :now
            where d.status in :statuses
            """)
    int markStaleAsFailed(
            @Param("failedStatus") DeploymentStatus failedStatus,
            @Param("statuses") Collection<DeploymentStatus> statuses,
            @Param("message") String message,
            @Param("now") Instant now
    );
}
