package az.baku.managesystemback.repository;

import az.baku.managesystemback.entity.AuthSession;
import az.baku.managesystemback.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {

    @Query("""
            select s from AuthSession s
            join fetch s.user
            where s.tokenHash = :tokenHash
              and s.revokedAt is null
              and s.expiresAt > :now
            """)
    Optional<AuthSession> findActiveByTokenHash(@Param("tokenHash") String tokenHash, @Param("now") Instant now);

    Optional<AuthSession> findByPublicIdAndUserId(@Param("publicId") UUID publicId, @Param("userId") long userId);

    List<AuthSession> findAllByUserIdAndRevokedAtIsNullAndExpiresAtAfter(long userId, Instant now);

    @Modifying
    @Query("""
            update AuthSession s
            set s.revokedAt = :now
            where s.user.id = :userId
              and s.revokedAt is null
              and s.expiresAt > :now
              and (:currentPublicId is null or s.publicId <> :currentPublicId)
            """)
    int revokeOtherActiveSessions(
            @Param("userId") long userId,
            @Param("currentPublicId") UUID currentPublicId,
            @Param("now") Instant now
    );

    @Modifying
    @Query("delete from AuthSession s where s.user = :user")
    void deleteAllByUser(@Param("user") User user);
}
