package az.baku.managesystemback.dto.request;

import az.baku.managesystemback.entity.SiteStatus;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Fatima Sultanova
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSiteRequest {

    @Positive(message = "Template id must be a positive number.")
    private Long templateId;

    @NotBlank(message = "Project name is required.")
    @Size(max = 150, message = "Project name must not exceed 150 characters.")
    private String projectName;

    @NotBlank(message = "URL slug is required.")
    @Size(max = 80, message = "URL slug must not exceed 80 characters.")
    @Pattern(
            regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
            message = "URL slug may contain only lowercase letters, digits and single hyphens."
    )
    private String slug;

    @NotNull(message = "Content is required.")
    private JsonNode content;

    private SiteStatus status;
}
