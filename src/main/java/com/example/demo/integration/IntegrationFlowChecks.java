package com.example.demo.integration;

import com.example.demo.config.AchievementDefinitions;
import com.example.demo.dto.ConfirmAttendanceRequest;
import com.example.demo.dto.CreateEventRequest;
import com.example.demo.dto.CreateEventResponse;
import com.example.demo.config.AchievementDefinitions.Entry;
import com.example.demo.dto.MyAchievementItem;
import com.example.demo.dto.MyCreatedEventItem;
import com.example.demo.entity.Achievement;
import com.example.demo.entity.Category;
import com.example.demo.entity.Event;
import com.example.demo.entity.User;
import com.example.demo.enums.EventStatus;
import com.example.demo.enums.ParticipationStatus;
import com.example.demo.enums.Role;
import com.example.demo.repository.AchievementRepository;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.EventParticipantsRepository;
import com.example.demo.repository.EventRepository;
import com.example.demo.repository.UserAchievementsRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AdminService;
import com.example.demo.service.EventService;
import com.example.demo.service.MeService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class IntegrationFlowChecks {

    private static final String ADMIN_AUTH_ID = "admin-seed-auth-id";

    private final MeService meService;
    private final AdminService adminService;
    private final EventService eventService;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final EventParticipantsRepository eventParticipantsRepository;
    private final AchievementRepository achievementRepository;
    private final UserAchievementsRepository userAchievementsRepository;

    public IntegrationFlowChecks(
            MeService meService,
            AdminService adminService,
            EventService eventService,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            EventRepository eventRepository,
            EventParticipantsRepository eventParticipantsRepository,
            AchievementRepository achievementRepository,
            UserAchievementsRepository userAchievementsRepository
    ) {
        this.meService = meService;
        this.adminService = adminService;
        this.eventService = eventService;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.eventRepository = eventRepository;
        this.eventParticipantsRepository = eventParticipantsRepository;
        this.achievementRepository = achievementRepository;
        this.userAchievementsRepository = userAchievementsRepository;
    }

    public void runAll() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User student = createStudent(suffix);
        User admin = userRepository.findByAuthId(ADMIN_AUTH_ID)
                .orElseThrow(() -> fail("Admin seed user not found"));
        Category category = categoryRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> fail("No categories in DB"));

        // baseline
        assertEmpty(meService.getMyEvents(student.getAuthId()).events(), "my events before any action");
        assertEmpty(meService.getMyCreatedEvents(student.getAuthId()).events(), "my created events before create");
        assertNoAchievements(student, "before any action");

        UUID approveEventId = createPendingEvent(student, category, "approve-" + suffix);
        UUID rejectEventId = createPendingEvent(student, category, "reject-" + suffix);

        assertCreatedEventStatus(student, approveEventId, EventStatus.pending.name());

        long eventCountBeforeModeration = eventRepository.count();

        // reject
        var rejectResponse = adminService.reject(rejectEventId, admin.getAuthId());
        assertEquals(rejectEventId, rejectResponse.eventId(), "reject response event_id");
        assertEquals(EventStatus.rejected.name(), rejectResponse.status(), "reject response status");
        assertEquals(admin.getId(), rejectResponse.moderatedBy(), "reject response moderated_by");
        assertEventStatus(rejectEventId, EventStatus.rejected);

        // approve
        var approveResponse = adminService.approve(approveEventId, admin.getAuthId());
        assertEquals(approveEventId, approveResponse.eventId(), "approve response event_id");
        assertEquals(EventStatus.approved.name(), approveResponse.status(), "approve response status");
        assertEquals(admin.getId(), approveResponse.moderatedBy(), "approve response moderated_by");
        assertEventStatus(approveEventId, EventStatus.approved);

        if (eventRepository.count() != eventCountBeforeModeration) {
            throw fail("approve/reject must not create extra event rows");
        }

        assertCreatedEventStatus(student, approveEventId, EventStatus.approved.name());
        assertCreatedEventStatus(student, rejectEventId, EventStatus.rejected.name());

        // creator achievement
        assertMyAchievementsExactly(student, List.of(AchievementDefinitions.FIRST_CREATED_EVENT),
                "after first approved event");

        // signup + getMyEvents
        eventService.SignUp(approveEventId, student.getAuthId(), ParticipationStatus.registered);

        var myEventsAfterSignup = meService.getMyEvents(student.getAuthId()).events();
        if (myEventsAfterSignup.size() != 1) {
            throw fail("getMyEvents must return 1 event after signup, got " + myEventsAfterSignup.size());
        }
        var myEvent = myEventsAfterSignup.get(0);
        assertEquals(approveEventId, myEvent.eventId(), "getMyEvents event_id");
        assertEquals(EventStatus.approved.name(), myEvent.status(), "getMyEvents event status");
        assertEquals(ParticipationStatus.registered.name(), myEvent.participationStatus(), "getMyEvents participation");

        // confirmAttendance
        int balanceBeforeAttendance = userRepository.findById(student.getId()).orElseThrow().getBalance();
        int attendanceReward = 5;
        int expectedAttendanceAchievementReward = AchievementDefinitions.FIRST_ATTENDANCE.rewardAmount();

        var attendanceResponse = adminService.confirmAttendance(
                approveEventId,
                student.getId(),
                admin.getAuthId(),
                new ConfirmAttendanceRequest(attendanceReward)
        );
        assertEquals(approveEventId, attendanceResponse.eventId(), "attendance response event_id");
        assertEquals(student.getId(), attendanceResponse.userId(), "attendance response user_id");
        assertEquals(ParticipationStatus.attended.name(), attendanceResponse.status(), "attendance response status");
        if (attendanceResponse.attendedAt() == null) {
            throw fail("attendance response attended_at must be set");
        }

        int expectedBalanceAfter = balanceBeforeAttendance + attendanceReward + expectedAttendanceAchievementReward;
        User studentAfter = userRepository.findById(student.getId()).orElseThrow();
        if (studentAfter.getBalance() != expectedBalanceAfter) {
            throw fail("student balance after attendance: expected "
                    + expectedBalanceAfter + " got " + studentAfter.getBalance());
        }

        var participation = eventParticipantsRepository
                .findByEventIdAndUserId(approveEventId, student.getId())
                .orElseThrow(() -> fail("participation not found after attendance"));
        if (participation.getStatus() != ParticipationStatus.attended) {
            throw fail("participation status must be attended");
        }
        if (participation.getConfirmedBy() == null
                || !participation.getConfirmedBy().getId().equals(admin.getId())) {
            throw fail("participation confirmed_by must be admin");
        }

        var myEventsAfterAttendance = meService.getMyEvents(student.getAuthId()).events().get(0);
        assertEquals(ParticipationStatus.attended.name(), myEventsAfterAttendance.participationStatus(),
                "getMyEvents after attendance");

        assertMyAchievementsExactly(student, List.of(
                AchievementDefinitions.FIRST_CREATED_EVENT,
                AchievementDefinitions.FIRST_ATTENDANCE
        ), "after first attendance");

        runFiveAttendancesAchievementScenario(student, admin, category, suffix);
    }

    private void runFiveAttendancesAchievementScenario(
            User student,
            User admin,
            Category category,
            String suffix
    ) {
        List<Entry> twoAchievements = List.of(
                AchievementDefinitions.FIRST_CREATED_EVENT,
                AchievementDefinitions.FIRST_ATTENDANCE
        );

        for (int attendanceNumber = 2; attendanceNumber <= 5; attendanceNumber++) {
            UUID eventId = createPendingEvent(student, category,
                    "attend-" + attendanceNumber + "-" + suffix);
            adminService.approve(eventId, admin.getAuthId());
            eventService.SignUp(eventId, student.getAuthId(), ParticipationStatus.registered);
            adminService.confirmAttendance(
                    eventId,
                    student.getId(),
                    admin.getAuthId(),
                    new ConfirmAttendanceRequest(1)
            );

            if (attendanceNumber < 5) {
                assertMyAchievementsExactly(student, twoAchievements,
                        "after attendance #" + attendanceNumber);
            }
        }

        assertMyAchievementsExactly(student, List.of(
                AchievementDefinitions.FIRST_CREATED_EVENT,
                AchievementDefinitions.FIRST_ATTENDANCE,
                AchievementDefinitions.FIVE_ATTENDANCES
        ), "after fifth attendance");
    }

    private User createStudent(String suffix) {
        User student = new User();
        student.setAuthId("it-flow-student-" + suffix);
        student.setEmail("it-flow-" + suffix + "@test.local");
        student.setName("IT Flow Student");
        student.setDescription("");
        student.setRole(Role.student);
        student.setBalance(0);
        return userRepository.save(student);
    }

    private UUID createPendingEvent(User student, Category category, String title) {
        CreateEventRequest request = new CreateEventRequest();
        request.setCategoryId(category.getId());
        request.setTitle(title);
        request.setDescription("integration flow test");
        request.setLocation("online");
        request.setStartTime(Instant.now().plusSeconds(3600));
        request.setEndTime(Instant.now().plusSeconds(7200));
        request.setMaxParticipants(20);
        CreateEventResponse created = eventService.CreateEvent(request, student.getAuthId());
        if (created.getStatus() != EventStatus.pending) {
            throw fail("student event must be created as pending");
        }
        return created.getEventId();
    }

    private void assertCreatedEventStatus(User student, UUID eventId, String expectedStatus) {
        List<MyCreatedEventItem> items = meService.getMyCreatedEvents(student.getAuthId()).events();
        MyCreatedEventItem item = items.stream()
                .filter(e -> eventId.equals(e.getEventId()))
                .findFirst()
                .orElseThrow(() -> fail("event " + eventId + " not found in getMyCreatedEvents"));
        if (!expectedStatus.equals(item.getStatus())) {
            throw fail("getMyCreatedEvents status for " + eventId
                    + " expected " + expectedStatus + " got " + item.getStatus());
        }
    }

    private void assertEventStatus(UUID eventId, EventStatus expected) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> fail("event not found: " + eventId));
        if (event.getStatus() != expected) {
            throw fail("event " + eventId + " status expected " + expected + " got " + event.getStatus());
        }
    }

    private void assertNoAchievements(User user, String context) {
        assertMyAchievementsExactly(user, List.of(), context);
    }

    private void assertMyAchievementsExactly(User user, List<Entry> expected, String context) {
        List<MyAchievementItem> apiAchievements = meService.getMyAchievements(user.getAuthId()).achievements();

        if (apiAchievements.size() != expected.size()) {
            throw fail(context + ": getMyAchievements size expected " + expected.size()
                    + " got " + apiAchievements.size() + " " + achievementTitles(apiAchievements));
        }

        long dbCount = userAchievementsRepository.countByUser_Id(user.getId());
        if (dbCount != expected.size()) {
            throw fail(context + ": user_achievements count expected " + expected.size() + " got " + dbCount);
        }

        for (Entry definition : expected) {
            Achievement achievement = achievementRepository.findByTitle(definition.title())
                    .orElseThrow(() -> fail(context + ": achievement not seeded: " + definition.title()));

            MyAchievementItem fromApi = apiAchievements.stream()
                    .filter(a -> definition.title().equals(a.title()))
                    .findFirst()
                    .orElseThrow(() -> fail(context + ": getMyAchievements missing " + definition.title()));

            assertEquals(achievement.getId(), fromApi.achievementId(), context + ": achievement_id");
            assertEquals(definition.title(), fromApi.title(), context + ": title");
            assertEquals(definition.description(), fromApi.description(), context + ": description");
            assertEquals(definition.rewardAmount(), fromApi.rewardAmount(), context + ": reward_amount");
            if (fromApi.awardedAt() == null) {
                throw fail(context + ": awarded_at must be set for " + definition.title());
            }

            if (!userAchievementsRepository.existsByUserIdAndAchievementId(user.getId(), achievement.getId())) {
                throw fail(context + ": user_achievements row missing for " + definition.title());
            }
        }

        for (MyAchievementItem item : apiAchievements) {
            boolean listed = expected.stream().anyMatch(e -> e.title().equals(item.title()));
            if (!listed) {
                throw fail(context + ": unexpected achievement in getMyAchievements: " + item.title());
            }
        }
    }

    private static String achievementTitles(List<MyAchievementItem> items) {
        return items.stream().map(MyAchievementItem::title).toList().toString();
    }

    private static void assertEmpty(List<?> list, String context) {
        if (!list.isEmpty()) {
            throw fail(context + ": expected empty list");
        }
    }

    private static void assertEquals(Object expected, Object actual, String context) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw fail(context + ": expected " + expected + " got " + actual);
        }
    }

    private static IllegalStateException fail(String message) {
        return new IllegalStateException(message);
    }
}
