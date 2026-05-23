package com.example.codecombat2026;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Full-context boot test. Disabled by default because it needs PostgreSQL
 * and Valkey running locally. To run: {@code ./mvnw test -Dgroups=integration}
 * or remove the {@code @Disabled} during a manual smoke check.
 */
@SpringBootTest
@Disabled("Requires running PostgreSQL + Valkey; run manually after starting infra")
class Codecombat2026ApplicationTests {

	@Test
	void contextLoads() {
	}

}
