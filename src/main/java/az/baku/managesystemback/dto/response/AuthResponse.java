package az.baku.managesystemback.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * @author Fatima Sultanova
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthResponse {
    private long id;
    private String name;
    private String surname;
    private String email;
    private String role;
    private boolean enabled;
    private boolean emailVerified;
    private LocalDateTime createdAt;
}
