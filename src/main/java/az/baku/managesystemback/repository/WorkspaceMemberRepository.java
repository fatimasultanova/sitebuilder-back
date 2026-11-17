package az.baku.managesystemback.repository;

import az.baku.managesystemback.entity.WorkspaceMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkspaceMemberRepository
        extends JpaRepository<WorkspaceMember, Long> {

    @EntityGraph(attributePaths = "user")
    Page<WorkspaceMember> findAllByWorkspaceId(
            long workspaceId,
            Pageable pageable
    );

    Optional<WorkspaceMember> findByInvitationTokenHash(String hash);

    boolean existsByWorkspaceIdAndUserId(
            long workspaceId,
            long userId
    );
}
