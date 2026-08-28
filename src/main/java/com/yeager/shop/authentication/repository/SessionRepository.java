package com.yeager.shop.authentication.repository;

import com.yeager.shop.authentication.entity.Session;
import com.yeager.shop.authentication.entity.SessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    @Query("""
            SELECT s.user.userId
            FROM Session s
            WHERE s.jti = :jti
            """)
    Optional<Long> findUserIdByJti(
            @Param("jti") String jti
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s
            FROM Session s
            WHERE s.jti = :jti
            """)
    Optional<Session> findForUpdateByJti(
            @Param("jti") String jti
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s
            FROM Session s
            WHERE s.sessionId = :sessionId
            """)
    Optional<Session> findForUpdateById(
            @Param("sessionId") Long sessionId
    );

    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query("""
            UPDATE Session s
            SET s.status = :revokedStatus
            WHERE s.user.userId = :userId
              AND s.status = :activeStatus
            """)
    int revokeActiveByUserId(
            @Param("userId") Long userId,
            @Param("activeStatus") SessionStatus activeStatus,
            @Param("revokedStatus") SessionStatus revokedStatus
    );
}
