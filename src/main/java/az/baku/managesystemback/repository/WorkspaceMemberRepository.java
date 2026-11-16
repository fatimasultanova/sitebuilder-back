package az.baku.managesystemback.repository;
import az.baku.managesystemback.entity.WorkspaceMember; import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable; import org.springframework.data.jpa.repository.JpaRepository;
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember,Long>{Page<WorkspaceMember> findAllByWorkspaceIdAndStatus(long id,String status,Pageable pageable); java.util.Optional<WorkspaceMember> findByInvitationTokenHash(String hash); boolean existsByWorkspaceIdAndUserId(long w,long u);}
