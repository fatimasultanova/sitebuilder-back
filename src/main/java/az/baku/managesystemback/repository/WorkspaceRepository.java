package az.baku.managesystemback.repository;

import az.baku.managesystemback.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * @author Fatima Sultanova
 */
public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    Optional<Workspace> findFirstByOwnerIdOrderByIdAsc(long ownerId);
}
