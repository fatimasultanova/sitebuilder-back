package az.baku.managesystemback.dto.request;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
/**
 * @author Fatima Sultanova
 */
@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
public class LoginRequest {
    @Email(message = "Yanlış email formatı")
    @NotBlank(message = "Email boş ola bilməz")
    private String email;

    @NotBlank(message = "Şifrə boş ola bilməz")
    private String password;
}
