package az.baku.managesystemback.service;

import az.baku.managesystemback.dto.request.CreateSupportTicketRequest;
import az.baku.managesystemback.entity.SupportTicket;
import az.baku.managesystemback.entity.User;
import az.baku.managesystemback.exception.ApiException;
import az.baku.managesystemback.repository.SupportTicketRepository;
import az.baku.managesystemback.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SupportTicketServiceTest {

    private SupportTicketRepository tickets;
    private UserRepository users;
    private NotificationService notificationService;
    private SupportTicketService service;

    @BeforeEach
    void setUp() {
        tickets = mock(SupportTicketRepository.class);
        users = mock(UserRepository.class);
        notificationService = mock(NotificationService.class);

        service = new SupportTicketService(
                tickets,
                users,
                notificationService
        );
    }

    @Test
    void createShouldSaveTicketAndCreateNotification() {
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .build();

        when(users.findById(1L))
                .thenReturn(Optional.of(user));

        when(tickets.save(any(SupportTicket.class)))
                .thenAnswer(invocation -> {
                    SupportTicket ticket = invocation.getArgument(0);
                    ticket.setId(10L);
                    ticket.setCreatedAt(Instant.now());
                    return ticket;
                });

        CreateSupportTicketRequest request =
                new CreateSupportTicketRequest(
                        "Login problem",
                        "technical",
                        "I cannot login to my account."
                );

        var result = service.create(1L, request);

        ArgumentCaptor<SupportTicket> captor =
                ArgumentCaptor.forClass(SupportTicket.class);

        verify(tickets).save(captor.capture());

        SupportTicket saved = captor.getValue();

        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getSubject()).isEqualTo("Login problem");
        assertThat(saved.getCategory()).isEqualTo("technical");
        assertThat(saved.getMessage()).isEqualTo("I cannot login to my account.");
        assertThat(result.id()).isEqualTo("10");

        verify(notificationService).createForUser(
                1L,
                "system",
                "success",
                "Support request received",
                "Your support request \"Login problem\" was created."
        );
    }

    @Test
    void listShouldReturnOnlyCurrentUsersTickets() {
        SupportTicket ticket = SupportTicket.builder()
                .id(5L)
                .subject("Test subject")
                .category("technical")
                .message("Test support message")
                .status("OPEN")
                .createdAt(Instant.now())
                .build();

        when(tickets.findAllByUserIdOrderByCreatedAtDesc(
                eq(1L),
                any()
        )).thenReturn(new PageImpl<>(List.of(ticket)));

        var result = service.list(1L, 0, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo("5");

        verify(tickets).findAllByUserIdOrderByCreatedAtDesc(
                eq(1L),
                any()
        );
    }

    @Test
    void getShouldReturnOwnedTicket() {
        SupportTicket ticket = SupportTicket.builder()
                .id(7L)
                .subject("Billing issue")
                .category("billing")
                .message("I have a billing problem.")
                .status("OPEN")
                .createdAt(Instant.now())
                .build();

        when(tickets.findByIdAndUserId(7L, 1L))
                .thenReturn(Optional.of(ticket));

        var result = service.get(1L, 7L);

        assertThat(result.id()).isEqualTo("7");
        assertThat(result.subject()).isEqualTo("Billing issue");
    }

    @Test
    void getShouldThrowNotFoundWhenTicketDoesNotBelongToUser() {
        when(tickets.findByIdAndUserId(7L, 2L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(2L, 7L))
                .isInstanceOf(ApiException.class);
    }
}
