package az.baku.managesystemback.service;

import az.baku.managesystemback.dto.request.CreateSupportTicketRequest;
import az.baku.managesystemback.dto.response.SupportTicketResponse;
import az.baku.managesystemback.entity.SupportTicket;
import az.baku.managesystemback.entity.User;
import az.baku.managesystemback.exception.ApiException;
import az.baku.managesystemback.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SupportTicketService {
    private final SupportTicketRepository tickets;
    private final UserRepository users;
    private final NotificationService notificationService;

    @Transactional
    public SupportTicketResponse create(
            long userId,
            CreateSupportTicketRequest request
    ) {
        SupportTicket ticket = SupportTicket.builder()
                .user(user(userId))
                .subject(request.subject().trim())
                .category(request.category().trim())
                .message(request.message().trim())
                .build();

        ticket = tickets.save(ticket);

        notificationService.createForUser(
                userId,
                "system",
                "success",
                "Support request received",
                "Your support request \"" + ticket.getSubject() + "\" was created."
        );

        return SupportTicketResponse.from(ticket);
    }

    @Transactional(readOnly = true)
    public Page<SupportTicketResponse> list(long uid, int page, int size) {
        return tickets.findAllByUserIdOrderByCreatedAtDesc(uid, PageRequest.of(page, size)).map(SupportTicketResponse::from);
    }

    @Transactional(readOnly = true)
    public SupportTicketResponse get(long uid, long id) {
        return tickets.findByIdAndUserId(id, uid).map(SupportTicketResponse::from).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Support ticket not found."));
    }

    private User user(long id) {
        return users.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
    }
}
