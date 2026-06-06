package com.example.demo.entity;

import com.example.demo.enums.EventStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "event")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    @Id
    @GeneratedValue
    @Column(name = "event_id", nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @ManyToOne
    @JoinColumn(name = "moderated_by", nullable = false)
    private User moderatedBy;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "title", nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "location", nullable = false, columnDefinition = "TEXT")
    private String location;

    @Column(name = "start_time", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant startTime;

    @Column(name = "end_time", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant endTime;

    @Column(name = "max_participants", nullable = false, columnDefinition = "INT")
    private int maxParticipants;

    @Column(name = "status", nullable = false, columnDefinition = "TEXT")
    @Enumerated(EnumType.STRING)
    private EventStatus status;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    @UpdateTimestamp
    private Instant updatedAt;









}
