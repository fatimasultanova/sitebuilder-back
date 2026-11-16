package az.baku.managesystemback.service;

import az.baku.managesystemback.config.GoogleOAuthProperties;
import az.baku.managesystemback.dto.response.LoginResponse;
import az.baku.managesystemback.entity.OAuthState;
import az.baku.managesystemback.exception.ApiException;
import az.baku.managesystemback.repository.OAuthStateRepository;
import az.baku.managesystemback.util.TokenUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    private final GoogleOAuthProperties properties;
    private final OAuthStateRepository oauthStateRepository;
    private final AuthService authService;
    private final RestClient.Builder restClientBuilder;

    @Transactional
    public String start(String returnTo) {
        return start("login", returnTo, null);
    }

    @Transactional
    public String start(String rawIntent, String returnTo, Long linkedUserId) {
        if (properties.getClientId().isBlank()
                || properties.getClientSecret().isBlank()) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Google sign-in is not configured."
            );
        }

        String intent = rawIntent == null
                ? "LOGIN"
                : rawIntent.trim().toUpperCase(Locale.ROOT);

        if (!Set.of("LOGIN", "REGISTER", "LINK").contains(intent)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported Google OAuth intent.");
        }

        if ("LINK".equals(intent) && linkedUserId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication is required to link Google.");
        }

        String rawState = TokenUtils.generateToken();

        oauthStateRepository.save(OAuthState.builder()
                .stateHash(TokenUtils.sha256(rawState))
                .expiresAt(Instant.now().plus(Duration.ofMinutes(10)))
                .returnTo(safeReturn(returnTo))
                .intent(intent)
                .linkedUserId("LINK".equals(intent) ? linkedUserId : null)
                .build());

        return UriComponentsBuilder
                .fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", properties.getClientId())
                .queryParam("redirect_uri", properties.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("state", rawState)
                .queryParam("prompt", "select_account")
                .build()
                .encode()
                .toUriString();
    }

    @Transactional
    public LoginResponse callback(String code, String rawState) {
        OAuthState state = oauthStateRepository
                .findByStateHash(TokenUtils.sha256(rawState.trim()))
                .orElseThrow(() -> new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "OAuth state is invalid or expired."
                ));

        if (!state.getExpiresAt().isAfter(Instant.now())) {
            oauthStateRepository.delete(state);
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "OAuth state is invalid or expired."
            );
        }

        oauthStateRepository.delete(state);

        Map<?, ?> tokenResponse = exchangeCode(code);
        Object accessToken = tokenResponse == null ? null : tokenResponse.get("access_token");

        if (accessToken == null) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "Google sign-in could not be completed."
            );
        }

        Map<?, ?> profile = restClientBuilder.build()
                .get()
                .uri("https://www.googleapis.com/oauth2/v3/userinfo")
                .headers(headers -> headers.setBearerAuth(accessToken.toString()))
                .retrieve()
                .body(Map.class);

        if (profile == null
                || !Boolean.TRUE.equals(profile.get("email_verified"))
                || profile.get("email") == null
                || profile.get("sub") == null) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "Google did not return a verified account."
            );
        }

        String email = profile.get("email").toString();
        String googleSubject = profile.get("sub").toString();
        String name = String.valueOf(
                profile.containsKey("name") ? profile.get("name") : "Google User"
        );

        if ("LINK".equals(state.getIntent())) {
            return authService.linkGoogleAccount(
                    state.getLinkedUserId(),
                    email,
                    googleSubject
            );
        }

        return authService.loginWithGoogle(
                state.getIntent(),
                email,
                name,
                googleSubject
        );
    }

    private Map<?, ?> exchangeCode(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());
        form.add("redirect_uri", properties.getRedirectUri());
        form.add("grant_type", "authorization_code");

        return restClientBuilder.build()
                .post()
                .uri("https://oauth2.googleapis.com/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);
    }

    private String safeReturn(String value) {
        if (value == null
                || !value.startsWith("/")
                || value.startsWith("//")
                || value.contains("\\")) {
            return "/";
        }

        return value;
    }
}
