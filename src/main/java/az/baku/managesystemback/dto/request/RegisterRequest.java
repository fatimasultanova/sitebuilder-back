package az.baku.managesystemback.dto.request;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * @author Fatima Sultanova
 */
@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
public class RegisterRequest {
    @NotBlank(message = "Ad boş ola bilməz")
    private String name;

    @NotBlank(message = "Soyad boş ola bilməz")
    private String surname;

    @Email(message = "Yanlış email formatı")
    @NotBlank(message = "Email boş ola bilməz")
    private String email;

    @Size(min = 6, message = "Şifrə ən az 6 simvol olmalıdır")
    @NotBlank(message = "Şifrə boş ola bilməz")
    private String password;
}
