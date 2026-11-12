package az.baku.managesystemback.repository;

import az.baku.managesystemback.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * @author Fatima Sultanova
 */
public interface PlanRepository extends JpaRepository<Plan, Long> {

    Optional<Plan> findByCode(String code);

    Optional<Plan> findByCodeIgnoreCase(String code);

    List<Plan> findByActiveTrueOrderByRankAsc();

    boolean existsByCode(String code);
}
