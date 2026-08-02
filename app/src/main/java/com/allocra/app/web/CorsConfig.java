package com.allocra.app.web;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Configurable CORS so browser-based clients (React, Flutter web) can call the
 * API from their own origin (PRD-NFR-010, ADR-011). Allowed origins are
 * environment-driven ({@code allocra.cors.allowed-origins}, comma-separated);
 * the default is none, so a browser client is only enabled when explicitly
 * configured. Non-browser clients (Flutter mobile) are unaffected either way.
 *
 * <p>
 * The filter runs at highest precedence — ahead of {@link TenantAuthFilter} —
 * so CORS preflight ({@code OPTIONS}) requests, which carry no credentials, are
 * answered before authentication rejects them.
 */
@Configuration
public class CorsConfig {

	@Bean
	public FilterRegistrationBean<CorsFilter> corsFilter(
			@Value("${allocra.cors.allowed-origins:}") String allowedOrigins) {
		CorsConfiguration config = new CorsConfiguration();
		for (String origin : parse(allowedOrigins)) {
			config.addAllowedOrigin(origin);
		}
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Tenant-Id"));
		// Bearer tokens (not cookies) are used, so credentialed CORS is not required.
		config.setAllowCredentials(false);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);

		FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(new CorsFilter(source));
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
		return registration;
	}

	private static List<String> parse(String csv) {
		if (csv == null || csv.isBlank()) {
			return List.of();
		}
		return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
	}
}
