package com.example.demo.config;

import com.example.demo.entity.Category;
import com.example.demo.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@Order(2)
public class CategoryInitializer implements CommandLineRunner {

    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    public void run(String... args) throws Exception {
        // Проверяем, есть ли уже категории
        if (categoryRepository.count() == 0) {
            System.out.println("Создание начальных категорий...");

            List<Category> categories = List.of(
                    createCategory("Конференция"),
                    createCategory("Воркшоп"),
                    createCategory("Семинар"),
                    createCategory("Вебинар"),
                    createCategory("Спорт"),
                    createCategory("Музыка"),
                    createCategory("Искусство"),
                    createCategory("Технологии"),
                    createCategory("Бизнес"),
                    createCategory("Образование"),
                    createCategory("Нетворкинг"),
                    createCategory("Выставка"),
                    createCategory("Фестиваль"),
                    createCategory("Квест"),
                    createCategory("Благотворительность")
            );

            categoryRepository.saveAll(categories);
            System.out.println("Создано " + categories.size() + " категорий");
        } else {
            System.out.println("Категории уже существуют, пропускаем инициализацию");
        }
    }

    private Category createCategory(String name) {
        Category category = new Category();
        category.setName(name);
        return category;
    }
}