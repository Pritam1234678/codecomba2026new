package com.example.codecombat2026;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class Codecombat2026Application {

	public static void main(String[] args) {
		// Use InheritableThreadLocal so worker threads inherit the security context
		org.springframework.security.core.context.SecurityContextHolder
			.setStrategyName(
				org.springframework.security.core.context.SecurityContextHolder.MODE_INHERITABLETHREADLOCAL
			);
		SpringApplication.run(Codecombat2026Application.class, args);
	}

}
