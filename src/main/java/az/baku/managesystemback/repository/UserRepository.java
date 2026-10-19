package az.baku.managesystemback.repository;
import az.baku.managesystemback.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
/**
 * @author Fatima Sultanova
 */
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
