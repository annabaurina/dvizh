package com.example.demo.config;

import com.example.demo.entity.Achievement;
import com.example.demo.repository.AchievementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(3)
public class AchievementInitializer implements CommandLineRunner {

    @Autowired
    private AchievementRepository achievementRepository;

    @Override
    public void run(String... args) {
        int created = 0;
        for (AchievementDefinitions.Entry definition : AchievementDefinitions.all()) {
            if (achievementRepository.findByTitle(definition.title()).isPresent()) {
                continue;
            }
            Achievement achievement = new Achievement();
            achievement.setTitle(definition.title());
            achievement.setDescription(definition.description());
            achievement.setRewardAmount(definition.rewardAmount());
            achievementRepository.save(achievement);
            created++;
        }
        if (created > 0) {
            System.out.println("Создано достижений: " + created);
        }
    }
}
