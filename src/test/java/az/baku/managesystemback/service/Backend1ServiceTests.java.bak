package az.baku.managesystemback.service;

import az.baku.managesystemback.config.AuthProperties;
import az.baku.managesystemback.dto.request.UpdateProfileRequest;
import az.baku.managesystemback.dto.request.WorkspaceRequest;
import az.baku.managesystemback.entity.AuthSession;
import az.baku.managesystemback.exception.ApiException;
import az.baku.managesystemback.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.ApplicationEventPublisher;


import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class Backend1ServiceTests {
    @Test
    void logoutRevokesMatchingSession() {
        AuthSessionRepository sessions = mock(AuthSessionRepository.class);
        AuthSession row = AuthSession.builder().tokenHash("hash").build();
        when(sessions.findByTokenHash(any())).thenReturn(Optional.of(row));
        AuthService auth = new AuthService(mock(UserRepository.class), sessions, mock(PasswordResetTokenRepository.class), mock(EmailVerificationTokenRepository.class), mock(PasswordEncoder.class), mock(ApplicationEventPublisher.class), new AuthProperties());
        auth.logout("token-value");
        assertThat(row.getRevokedAt()).isNotNull();
        verify(sessions).findByTokenHash(any());
    }

    @Test
    void profileUpdateChangesOnlyProfileFields() {
        UserRepository users = mock(UserRepository.class);
        var user = az.baku.managesystemback.entity.User.builder().id(7).name("Old").surname("Surname").email("a@example.com").role("ROLE_USER").build();
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(users.save(any())).thenAnswer(x -> x.getArgument(0));
        ProfileService service = new ProfileService(users);
        var result = service.update(7, new UpdateProfileRequest(" New ", " Name "));
        assertThat(result.name()).isEqualTo("New");
        assertThat(result.surname()).isEqualTo("Name");
        assertThat(user.getEmail()).isEqualTo("a@example.com");
        assertThat(user.getRole()).isEqualTo("ROLE_USER");
    }

    @Test
    void workspaceUpdateIsOwnerScoped() {
        WorkspaceRepository workspaces = mock(WorkspaceRepository.class);
        when(workspaces.findByIdAndOwnerId(4L, 8L)).thenReturn(Optional.empty());
        WorkspaceService service = new WorkspaceService(workspaces, mock(WorkspaceMemberRepository.class), mock(UserRepository.class), mock(ApplicationEventPublisher.class),mock(NotificationService.class));
        assertThatThrownBy(() -> service.update(8, 4, new WorkspaceRequest("Attempt"))).isInstanceOf(ApiException.class);
        verify(workspaces).findByIdAndOwnerId(4L, 8L);
    }
}
