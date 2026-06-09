package com.example.demo.integration;

import com.example.demo.entity.User;
import com.example.demo.enums.Role;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.DemoUserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.UUID;

final class IntegrationTestSupport {

    private IntegrationTestSupport() {}

    static final String ADMIN_AUTH_ID = "admin-seed-auth-id";
    static final String IT_PREFIX = "it-test-";

    static void setCurrentUser(String authId, String email) {
        DemoUserDetails details = new DemoUserDetails(authId, email);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, Collections.emptyList())
        );
    }

    static void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    static User createStudent(UserRepository userRepository, String suffix) {
        User student = new User();
        student.setAuthId(IT_PREFIX + "student-" + suffix);
        student.setEmail(IT_PREFIX + suffix + "@test.local");
        student.setName("IT Student " + suffix);
        student.setDescription("");
        student.setRole(Role.student);
        student.setBalance(0);
        return userRepository.save(student);
    }

    static String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
