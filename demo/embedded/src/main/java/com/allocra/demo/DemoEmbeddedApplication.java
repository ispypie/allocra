package com.allocra.demo;

import com.allocra.app.AllocraApplication;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Self-contained demo launcher (no Docker). Starts a real PostgreSQL from a
 * downloaded binary, then boots the Allocra application against it on
 * {@code http://localhost:8080} with the {@code demo} profile active. Flyway
 * applies the schema and {@link DemoDataSeeder} seeds a sample organisation and
 * prints a cheat sheet.
 *
 * <p>
 * The embedded database is ephemeral — it starts fresh on every run and is
 * discarded on exit. Authentication uses the local stub verifier (the bearer
 * token is the user id), so no Firebase is needed to explore the API.
 */
public final class DemoEmbeddedApplication {

	private DemoEmbeddedApplication() {
	}

	public static void main(String[] args) throws IOException {
		EmbeddedPostgres postgres = EmbeddedPostgres.builder().start();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> closeQuietly(postgres)));

		String jdbcUrl = postgres.getJdbcUrl("postgres", "postgres");
		new SpringApplicationBuilder(AllocraApplication.class).profiles("demo").run("--server.port=8080",
				"--spring.datasource.url=" + jdbcUrl, "--spring.datasource.username=postgres",
				"--spring.datasource.password=postgres",
				// Allow common local front-end dev origins (React :3000, Vite :5173).
				"--allocra.cors.allowed-origins=http://localhost:3000,http://localhost:5173");
	}

	private static void closeQuietly(EmbeddedPostgres postgres) {
		try {
			postgres.close();
		} catch (IOException ignored) {
			// best-effort shutdown of the ephemeral demo database
		}
	}
}
