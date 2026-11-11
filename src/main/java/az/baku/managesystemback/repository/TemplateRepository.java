package az.baku.managesystemback.repository;

import az.baku.managesystemback.entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * @author Fatima Sultanova
 */
public interface TemplateRepository extends JpaRepository<Template, Long> {

    @Query("select t from Template t where t.active = true order by t.sortOrder asc, t.id asc")
    List<Template> findActiveTemplates();
}
