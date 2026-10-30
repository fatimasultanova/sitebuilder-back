package az.baku.managesystemback.security;

import az.baku.managesystemback.entity.AuthSession;
import az.baku.managesystemback.entity.User;
import az.baku.managesystemback.repository.AuthSessionRepository;
import az.baku.managesystemback.util.RoleUtils;
import az.baku.managesystemback.util.TokenUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthSessionRepository authSessionRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null
                && header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String rawToken = header.substring(BEARER_PREFIX.length()).trim();

            if (!rawToken.isEmpty()) {
                authenticate(rawToken, request);
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(String rawToken, HttpServletRequest request) {
        Optional<AuthSession> sessionOptional = authSessionRepository
                .findActiveByTokenHash(TokenUtils.sha256(rawToken), Instant.now());

        if (sessionOptional.isEmpty()) {
            return;
        }

        AuthSession session = sessionOptional.get();
        User user = session.getUser();

        if (!user.isEnabled() || !user.isEmailVerified()) {
            return;
        }

        String role;
        try {
            role = RoleUtils.normalize(user.getRole());
        } catch (IllegalArgumentException ex) {
            return;
        }

        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail(), role);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority(RoleUtils.toAuthority(role)))
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
