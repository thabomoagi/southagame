package com.thabo.howsouthaareyou.qna.repository;

import com.thabo.howsouthaareyou.qna.entity.Attempt;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface LeaderboardRepository extends JpaRepository<Attempt, UUID> {

  @Query("""
      SELECT a.user.id AS userId,
             a.user.username AS username,
             a.user.profilePictureUrl AS profilePictureUrl,
             SUM(a.score) AS score
      FROM Attempt a
      WHERE a.completedAt IS NOT NULL
        AND a.score IS NOT NULL
      GROUP BY a.user.id,
               a.user.username,
               a.user.profilePictureUrl
      ORDER BY SUM(a.score) DESC,
               a.user.username ASC
      """)
  List<LeaderboardProjection> findTopAll(Pageable pageable);

  @Query("""
      SELECT a.user.id AS userId,
             a.user.username AS username,
             a.user.profilePictureUrl AS profilePictureUrl,
             SUM(a.score) AS score
      FROM Attempt a
      WHERE a.completedAt IS NOT NULL
        AND a.score IS NOT NULL
        AND a.startedAt >= :startDate
      GROUP BY a.user.id,
               a.user.username,
               a.user.profilePictureUrl
      ORDER BY SUM(a.score) DESC,
               a.user.username ASC
      """)
  List<LeaderboardProjection> findTopSince(
      @Param("startDate") LocalDateTime startDate,
      Pageable pageable);

  @Query("""
      SELECT COUNT(u)
      FROM User u
      WHERE u.id <> :userId
        AND u.id IN (
            SELECT a.user.id
            FROM Attempt a
            WHERE a.completedAt IS NOT NULL
              AND a.score IS NOT NULL
            GROUP BY a.user.id
            HAVING SUM(a.score) > (
                  SELECT COALESCE(SUM(a2.score), 0)
                  FROM Attempt a2
                  WHERE a2.user.id = :userId
                    AND a2.completedAt IS NOT NULL
                    AND a2.score IS NOT NULL
              )
        )
      """)
  long countPlayersAboveAllTimeScore(@Param("userId") UUID userId);

  @Query("""
      SELECT COUNT(u)
      FROM User u
      WHERE u.id <> :userId
        AND u.id IN (
            SELECT a.user.id
            FROM Attempt a
            WHERE a.completedAt IS NOT NULL
              AND a.score IS NOT NULL
              AND a.startedAt >= :startDate
            GROUP BY a.user.id
            HAVING SUM(a.score) > (
                  SELECT COALESCE(SUM(a2.score), 0)
                  FROM Attempt a2
                  WHERE a2.user.id = :userId
                    AND a2.completedAt IS NOT NULL
                    AND a2.score IS NOT NULL
                    AND a2.startedAt >= :startDate
              )
        )
      """)
  long countPlayersAboveScoreSince(
      @Param("userId") UUID userId,
      @Param("startDate") LocalDateTime startDate);
}