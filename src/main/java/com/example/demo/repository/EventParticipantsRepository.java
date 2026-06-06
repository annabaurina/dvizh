package com.example.demo.repository;

import com.example.demo.dto.MyEventItem;
import com.example.demo.entity.EventParticipants;
import com.example.demo.entity.User;
import com.example.demo.enums.ParticipationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventParticipantsRepository extends JpaRepository<EventParticipants, Long> {

    boolean existsByUserIdAndEventId(UUID userId, UUID eventId);

        @Query("""
                SELECT COUNT(ep) FROM EventParticipants ep
                WHERE ep.event.id = :eventId AND ep.status = :registered
                """)
        int countByEventIdAndStatus(
                @Param("eventId") UUID eventId,
                @Param("registered") ParticipationStatus registered
        );

        @Query("""
            SELECT new com.example.demo.dto.MyEventItem(
                e.id, e.title, str(e.status), str(ep.status))
            FROM EventParticipants ep
            JOIN ep.event e
            WHERE ep.user.id = :userId
            """)
        List<MyEventItem> findMyEventItemsByUserId(@Param("userId") UUID userId);

        @Query("""
            SELECT ep FROM EventParticipants ep
            WHERE ep.event.id = :eventId AND ep.user.id = :userId
            """)
        Optional<EventParticipants> findByEventIdAndUserId(
                @Param("eventId") UUID eventId,
                @Param("userId") UUID userId
        );

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query("""
            UPDATE EventParticipants ep
            SET ep.status = :attended,
                ep.confirmedBy = :admin,
                ep.confirmedAt = :confirmedAt
            WHERE ep.event.id = :eventId AND ep.user.id = :userId
            """)
        int markAsAttended(
                @Param("eventId") UUID eventId,
                @Param("userId") UUID userId,
                @Param("admin") User admin,
                @Param("confirmedAt") Instant confirmedAt,
                @Param("attended") ParticipationStatus attended
        );

        @Query("""
            SELECT COUNT(ep) FROM EventParticipants ep
            WHERE ep.user.id = :userId AND ep.status = :attended
            """)
        long countByUserIdAndStatus(
                @Param("userId") UUID userId,
                @Param("attended") ParticipationStatus attended
        );

}
