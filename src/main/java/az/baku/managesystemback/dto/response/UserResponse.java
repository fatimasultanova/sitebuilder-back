package az.baku.managesystemback.dto.response;

import az.baku.managesystemback.entity.User;
import az.baku.managesystemback.util.RoleUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserResponse {

    private final String id;
    private final String name;
    private final String email;
    private final String role;

    public static UserResponse from(User user) {
        return new UserResponse(
                Long.toString(user.getId()),
                user.getName(),
                user.getEmail(),
                RoleUtils.normalize(user.getRole())
        );
    }
}
