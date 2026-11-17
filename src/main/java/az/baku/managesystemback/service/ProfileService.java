package az.baku.managesystemback.service;

import az.baku.managesystemback.dto.request.UpdateProfileRequest;
import az.baku.managesystemback.dto.response.ProfileResponse;
import az.baku.managesystemback.entity.User;
import az.baku.managesystemback.exception.ApiException;
import az.baku.managesystemback.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final UserRepository users;

    @Transactional(readOnly = true)
    public ProfileResponse get(long id) {
        return ProfileResponse.from(user(id));
    }

    @Transactional
    public ProfileResponse update(long id, UpdateProfileRequest r) {
        User u = user(id);
        u.setName(r.name().trim());
        u.setSurname(r.surname() == null ? null : r.surname().trim());
        return ProfileResponse.from(users.save(u));
    }

    @Transactional
    public ProfileResponse avatar(long id, MultipartFile f) {
        if (f.isEmpty() || f.getSize() > 5 * 1024 * 1024)
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Avatar must be a non-empty image up to 5 MB.");
        String type = f.getContentType();
        if (type == null || !java.util.Set.of("image/jpeg", "image/png", "image/webp").contains(type))
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Avatar must be JPEG, PNG or WebP.");
        try {
            byte[] bytes = f.getBytes();

            if (!valid(type, bytes))
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Uploaded file content is not a supported image.");
            User u = user(id);
            u.setAvatarUrl("data:" + type + ";base64," + Base64.getEncoder().encodeToString(bytes));
            return ProfileResponse.from(users.save(u));
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Could not read uploaded avatar.");
        }
    }

    @Transactional
    public void deleteAvatar(long id) {
        User u = user(id);
        u.setAvatarUrl(null);
        users.save(u);
    }

    private boolean valid(String t, byte[] b) {
        if (b.length < 4) return false;
        if (t.equals("image/png")) return b.length >= 8 && b[0] == (byte) 137 && b[1] == 80 && b[2] == 78 && b[3] == 71;
        if (t.equals("image/jpeg"))
            return b[0] == (byte) 255 && b[1] == (byte) 216 && b[b.length - 2] == (byte) 255 && b[b.length - 1] == (byte) 217;
        return b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
    }

    private User user(long id) {
        return users.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
    }
}
