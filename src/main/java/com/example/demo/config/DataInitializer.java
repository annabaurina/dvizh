package com.example.demo.config;

import com.example.demo.entity.User;
import com.example.demo.enums.Role;
import com.example.demo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Создаёт пользователя-администратора при запуске приложения, если его ещё нет.
 *
 * Зачем: чтобы сразу был аккаунт с ролью admin для эндпоинтов 3 и 19
 * (смена ролей и список пользователей) — иначе первый зашедший через OAuth
 * пользователь получает роль student и админских прав ни у кого нет.
 *
 * auth_id берётся фиксированный (ADMIN_AUTH_ID). Чтобы реально заходить под
 * этим админом через фронтенд, нужно, чтобы auth_id из токена Supabase
 * совпадал с этим значением — поэтому это значение, скорее всего, нужно будет
 * заменить на реальный sub (UUID) конкретного пользователя из Supabase.
 */
@Component
@Order(1)
public class DataInitializer implements CommandLineRunner {

    // TODO: заменить на реальный auth_id (sub из Supabase) того, кто должен быть админом.
    private static final String ADMIN_AUTH_ID = "admin-seed-auth-id";

    private final UserRepository userRepository;

    public DataInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByAuthId(ADMIN_AUTH_ID).isPresent()) {
            return; // админ уже создан — ничего не делаем
        }
        User admin = new User();
        admin.setAuthId(ADMIN_AUTH_ID);
        admin.setEmail("admin@example.com");
        admin.setName("Администратор");
        admin.setDescription("");
        admin.setRole(Role.admin);
        admin.setBalance(0);
        userRepository.save(admin);
    }
}
