package com.allocra.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application entry point for the Allocra modular monolith.
 *
 * <p>
 * Component scanning is rooted at {@code com.allocra} so beans contributed by
 * each module are discovered by the single deployable. The application is
 * stateless and horizontally scalable (PRD-NFR-001); PostgreSQL is the
 * authority for bookings and reservations.
 */
@SpringBootApplication(scanBasePackages = "com.allocra")
public class AllocraApplication {

	public static void main(String[] args) {
		SpringApplication.run(AllocraApplication.class, args);
	}
}
