package com.allocra.app.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Enforces module-boundary and purity rules (PRD-NFR-007). These rules make the
 * accepted architecture (ADR-001, docs/03-TECHNICAL-SPECIFICATION.md §2.3)
 * self-checking so the next contributor cannot silently violate it.
 */
@AnalyzeClasses(packages = "com.allocra", importOptions = {ImportOption.DoNotIncludeTests.class})
class ArchitectureTest {

	@ArchTest
	static final ArchRule scheduling_domain_must_not_depend_on_web = noClasses().that()
			.resideInAPackage("com.allocra.scheduling..").should().dependOnClassesThat()
			.resideInAnyPackage("org.springframework.web..", "org.springframework.web.bind..",
					"org.springframework.web.servlet..", "jakarta.servlet..")
			.as("PRD-SCH-001 / PRD-NFR-007: the scheduling domain must not depend on HTTP");

	@ArchTest
	static final ArchRule scheduling_domain_must_not_depend_on_persistence = noClasses().that()
			.resideInAPackage("com.allocra.scheduling..").should().dependOnClassesThat()
			.resideInAnyPackage("jakarta.persistence..", "org.hibernate..")
			.as("PRD-NFR-007: infrastructure/JPA entities must not leak into the scheduling domain");

	@ArchTest
	static final ArchRule scheduling_domain_must_not_depend_on_firebase = noClasses().that()
			.resideInAPackage("com.allocra.scheduling..").should().dependOnClassesThat()
			.resideInAnyPackage("com.google.firebase..", "com.google.auth..")
			.as("PRD-NFR-007: the scheduling domain must not depend on the Firebase SDK");

	@ArchTest
	static final ArchRule nothing_outside_app_may_depend_on_app = noClasses().that()
			.resideOutsideOfPackage("com.allocra.app..").should().dependOnClassesThat()
			.resideInAPackage("com.allocra.app..")
			.as("Only the app module wires the system; modules must not depend on app");
}
