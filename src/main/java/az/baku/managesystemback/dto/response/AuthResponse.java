package az.baku.managesystemback.dto.response;
import lombok.*;
import java.time.LocalDateTime;
/**
 * @author Fatima Sultanova
 */
@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
public class AuthResponse {
    private long id;
    private String name;
    private String surname;
    private String email;
    private String role;
    private boolean enabled;
    private LocalDateTime createdAt;
}
