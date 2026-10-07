package com.thabo.howsouthaareyou.qna.repository;

import com.thabo.howsouthaareyou.qna.entity.Attempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface AttemptRepository extends JpaRepository<Attempt, UUID> {

        Optional<Attempt> findByIdAndUserId(UUID id, UUID userId);

        Page<Attempt> findByUserIdOrderByStartedAtDesc(UUID userId, Pageable pageable);

        long countByUserId(UUID userId);

        @Query("""
                        SELECT COUNT(a)
                        FROM Attempt a
                        WHERE a.user.id = :userId
                        AND a.startedAt >= :startDate
                        """)
        long countAttemptsStartedSince(
                        @Param("userId") UUID userId,
                        @Param("startDate") LocalDateTime startDate);

        @Query("""
                        SELECT COALESCE(SUM(a.score), 0)
                        FROM Attempt a
                        WHERE a.user.id = :userId
                        AND a.completedAt IS NOT NULL
                        AND a.score IS NOT NULL
                        """)
        Integer findTotalScoreByUserId(@Param("userId") UUID userId);

        @Query("""
                        SELECT COALESCE(SUM(a.score), 0)
                        FROM Attempt a
                        WHERE a.user.id = :userId
                        AND a.completedAt IS NOT NULL
                        AND a.score IS NOT NULL
                        AND a.startedAt >= :startDate
                        """)
        Integer findTotalScoreSince(
                        @Param("userId") UUID userId,
                        @Param("startDate") LocalDateTime startDate);
}