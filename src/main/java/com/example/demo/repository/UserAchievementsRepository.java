package com.example.demo.repository;

import com.example.demo.dto.MyAchievementItem;
import com.example.demo.entity.UserAchievements;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserAchievementsRepository extends JpaRepository<UserAchievements, Long> {

    @Query("""
            SELECT new com.example.demo.dto.MyAchievementItem(
                a.id, a.title, a.description, a.rewardAmount, ua.awardedAt)
            FROM UserAchievements ua
            JOIN ua.achievement a
            WHERE ua.user.id = :userId
            """)
    List<MyAchievementItem> findMyAchievementItemsByUserId(@Param("userId") UUID userId);

    @Query("""
            SELECT COUNT(ua) > 0 FROM UserAchievements ua
            WHERE ua.user.id = :userId AND ua.achievement.id = :achievementId
            """)
    boolean existsByUserIdAndAchievementId(
            @Param("userId") UUID userId,
            @Param("achievementId") UUID achievementId
    );

    long countByUser_Id(UUID userId);
}
