package com.example.codecombat2026.config;

import com.example.codecombat2026.entity.Role;
import com.example.codecombat2026.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds the roles table on startup if it's empty.
 * ROLE_USER  → id 1
 * ROLE_ADMIN → id 2
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        if (roleRepository.count() == 0) {
            roleRepository.save(new Role(null, Role.ERole.ROLE_USER));
            roleRepository.save(new Role(null, Role.ERole.ROLE_ADMIN));
            System.out.println("✅ Roles seeded: ROLE_USER (id=1), ROLE_ADMIN (id=2)");
        } else {
            System.out.println("ℹ️  Roles already exist, skipping seed.");
        }
    }
}
