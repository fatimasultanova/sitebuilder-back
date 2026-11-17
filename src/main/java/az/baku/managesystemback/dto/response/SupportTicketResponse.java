package az.baku.managesystemback.dto.response;

import az.baku.managesystemback.entity.SupportTicket;

import java.time.Instant;

public record SupportTicketResponse(String id, String subject, String category, String message, String status,
                                    Instant createdAt) {
    public static SupportTicketResponse from(SupportTicket t) {
        return new SupportTicketResponse(Long.toString(t.getId()), t.getSubject(), t.getCategory(), t.getMessage(), t.getStatus(), t.getCreatedAt());
    }
}
