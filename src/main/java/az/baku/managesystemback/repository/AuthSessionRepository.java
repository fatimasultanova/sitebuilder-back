package az.baku.managesystemback.repository;

import az.baku.managesystemback.entity.AuthSession;
import az.baku.managesystemback.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {

    Optional<AuthSession> findByTokenHash(String tokenHash);

    @Query("""
            select s from AuthSession s
            join fetch s.user
            where s.tokenHash = :tokenHash
              and s.revokedAt is null
              and s.expiresAt > :now
            """)
    Optional<AuthSession> findActiveByTokenHash(
            @Param("tokenHash") String tokenHash,
            @Param("now") Instant now
    );

    Page<AuthSession> findAllByUserIdAndRevokedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
            long userId,
            Instant now,
            Pageable pageable
    );

    Optional<AuthSession> findByIdAndUserId(long sessionId, long userId);

    @Modifying
    @Query("""
            update AuthSession s
            set s.revokedAt = :now
            where s.user.id = :userId
              and s.tokenHash <> :currentTokenHash
              and s.revokedAt is null
              and s.expiresAt > :now
            """)
    int revokeOtherActiveSessions(
            @Param("userId") long userId,
            @Param("currentTokenHash") String currentTokenHash,
            @Param("now") Instant now
    );
    Page<AuthSession> findAllByUserIdOrderByCreatedAtDesc(
            long userId,
            Pageable pageable
    );

    @Modifying
    @Query("delete from AuthSession s where s.user = :user")
    void deleteAllByUser(@Param("user") User user);
}
