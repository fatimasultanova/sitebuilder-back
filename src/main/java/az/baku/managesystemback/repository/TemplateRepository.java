package az.baku.managesystemback.repository;

import az.baku.managesystemback.entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * @author Fatima Sultanova
 */
public interface TemplateRepository extends JpaRepository<Template, Long> {

    @Query("select t from Template t where t.isPublished = true order by t.name asc")
    List<Template> findPublishedTemplates();

    @Query("select t from Template t where t.id = :id and t.isPublished = true")
    Optional<Template> findPublishedById(@Param("id") long id);
}
