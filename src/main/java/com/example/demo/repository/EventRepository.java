package com.example.demo.repository;

import com.example.demo.dto.GetEvent;
import com.example.demo.dto.MyCreatedEventItem;
import com.example.demo.dto.MyEventItem;
import com.example.demo.entity.Event;
import com.example.demo.enums.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {


    boolean existsByCreatorIdAndIdAndStatus(UUID creatorId, UUID eventId, EventStatus status);

    @Query("""
            SELECT new com.example.demo.dto.MyCreatedEventItem(
                e.id, e.title, str(e.status), e.createdAt)
            FROM Event e
            WHERE e.creator.id = :userId
            """)
    List<MyCreatedEventItem> findMyCreatedEventItemsByCreatorId(@Param("userId") UUID userId);

    @Query("""
            SELECT COUNT(e) FROM Event e
            WHERE e.creator.id = :creatorId AND e.status = :status
            """)
    long countByCreatorIdAndStatus(
            @Param("creatorId") UUID creatorId,
            @Param("status") EventStatus status
    );
}
