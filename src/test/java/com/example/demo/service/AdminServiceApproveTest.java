package com.example.demo.service;

import com.example.demo.entity.Event;
import com.example.demo.entity.User;
import com.example.demo.enums.EventStatus;
import com.example.demo.enums.Role;
import com.example.demo.repository.AchievementRepository;
import com.example.demo.repository.EventParticipantsRepository;
import com.example.demo.repository.EventRepository;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.repository.UserAchievementsRepository;
import com.example.demo.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceApproveTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventParticipantsRepository eventParticipantsRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AchievementRepository achievementRepository;
    @Mock
    private UserAchievementsRepository userAchievementsRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private AdminService adminService;

    @Test
    void approveUpdatesExistingEvent_doesNotCreateNewOne() {
        UUID eventId = UUID.randomUUID();
        User creator = new User();
        creator.setId(UUID.randomUUID());
        creator.setAuthId("student-1");
        creator.setEmail("s@test.local");
        creator.setName("Student");
        creator.setDescription("");
        creator.setRole(Role.student);
        creator.setBalance(0);

        User admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setAuthId("admin-seed-auth-id");
        admin.setEmail("admin@test.local");
        admin.setName("Admin");
        admin.setDescription("");
        admin.setRole(Role.admin);
        admin.setBalance(0);

        Event event = new Event();
        event.setId(eventId);
        event.setCreator(creator);
        event.setTitle("Test event");
        event.setStatus(EventStatus.pending);
        event.setUpdatedAt(Instant.now());

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(userRepository.findByAuthId("admin-seed-auth-id")).thenReturn(Optional.of(admin));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.countByCreatorIdAndStatus(creator.getId(), EventStatus.approved)).thenReturn(1L);
        when(achievementRepository.findByTitle(any())).thenReturn(Optional.empty());

        adminService.approve(eventId, "admin-seed-auth-id");

        ArgumentCaptor<Event> savedEvent = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository, times(1)).save(savedEvent.capture());

        assertEquals(eventId, savedEvent.getValue().getId());
        assertEquals(EventStatus.approved, savedEvent.getValue().getStatus());
        assertEquals(admin.getId(), savedEvent.getValue().getModeratedBy().getId());
    }
}
