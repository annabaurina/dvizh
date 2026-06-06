package com.example.demo.entity;

import com.example.demo.enums.ParticipationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "event_participants")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventParticipants {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Event event;

    @ManyToOne
    private User user;

    @Column(name = "status", nullable = false, columnDefinition = "TEXT")
    @Enumerated(EnumType.STRING)
    private ParticipationStatus status;

    @ManyToOne
    @JoinColumn(name = "confirmed_by")
    private User confirmedBy;

    @Column(name = "confirmed_at", columnDefinition = "TIMESTAMPTZ")
    private Instant confirmedAt;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    @CreationTimestamp
    private Instant createdAt;
}
