package com.allocra.app.docs;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Lightweight documentation validation (PRD-NFR-008). Fails the build when
 * project documentation drifts out of alignment with requirements. It does NOT
 * attempt to prove prose and code are semantically identical — it enforces
 * explicit traceability instead.
 *
 * <p>
 * Checks: required docs exist; every referenced ADR file exists (no broken ADR
 * links in the decision register or elsewhere); every PRD id referenced by the
 * traceability document is defined in the PRD; every PRD id referenced from
 * Java source (e.g. test {@code @DisplayName}s) is a known PRD id.
 */
class DocumentationValidationTest {

	private static final Pattern PRD_ID = Pattern.compile("PRD-[A-Z]{2,4}-\\d{3}");
	private static final Pattern ADR_REF = Pattern.compile("ADR-(\\d{3})");

	private static final List<String> REQUIRED_DOCS = List.of("PROJECT_CONTEXT.md", "docs/00-VISION.md",
			"docs/01-PRD.md", "docs/02-DOMAIN-MODEL.md", "docs/03-TECHNICAL-SPECIFICATION.md",
			"docs/04-DECISION-REGISTER.md", "docs/05-GLOSSARY.md", "docs/06-FUTURE-IDEAS.md",
			"docs/07-OPEN-QUESTIONS.md", "docs/08-REQUIREMENTS-TRACEABILITY.md");

	private final Path repoRoot = locateRepoRoot();

	@Test
	@DisplayName("PRD-NFR-008: all required project documents exist")
	void requiredDocumentsExist() {
		List<String> missing = new ArrayList<>();
		for (String doc : REQUIRED_DOCS) {
			if (!Files.isRegularFile(repoRoot.resolve(doc))) {
				missing.add(doc);
			}
		}
		assertTrue(missing.isEmpty(), "Missing required documentation: " + missing);
	}

	@Test
	@DisplayName("PRD-NFR-008: every referenced ADR file exists (no broken ADR links)")
	void referencedAdrsExist() {
		Set<String> referenced = new TreeSet<>();
		for (Path md : markdownFiles()) {
			Matcher m = ADR_REF.matcher(read(md));
			while (m.find()) {
				referenced.add(m.group(1));
			}
		}
		List<String> broken = new ArrayList<>();
		for (String num : referenced) {
			boolean exists;
			try (Stream<Path> s = Files.list(repoRoot.resolve("docs/adr"))) {
				exists = s.anyMatch(p -> p.getFileName().toString().startsWith("ADR-" + num + "-"));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			if (!exists) {
				broken.add("ADR-" + num);
			}
		}
		assertTrue(broken.isEmpty(), "Referenced ADR(s) have no matching file in docs/adr: " + broken);
	}

	@Test
	@DisplayName("PRD-NFR-008: every PRD id in the traceability document is defined in the PRD")
	void traceabilityReferencesKnownRequirements() {
		Set<String> defined = idsIn(read(repoRoot.resolve("docs/01-PRD.md")));
		assertTrue(defined.size() > 20, "Expected the PRD to define many requirements; found " + defined.size());

		Set<String> referenced = idsIn(read(repoRoot.resolve("docs/08-REQUIREMENTS-TRACEABILITY.md")));
		Set<String> unknown = new TreeSet<>(referenced);
		unknown.removeAll(defined);
		assertTrue(unknown.isEmpty(), "Traceability references unknown PRD id(s): " + unknown);
	}

	@Test
	@DisplayName("PRD-NFR-008: every PRD id referenced from Java source is a known requirement")
	void sourceReferencesKnownRequirements() {
		Set<String> defined = idsIn(read(repoRoot.resolve("docs/01-PRD.md")));
		Set<String> unknown = new TreeSet<>();
		for (Path java : javaFiles()) {
			for (String id : idsIn(read(java))) {
				if (!defined.contains(id)) {
					unknown.add(id + " (in " + repoRoot.relativize(java) + ")");
				}
			}
		}
		assertTrue(unknown.isEmpty(), "Source references unknown PRD id(s): " + unknown);
	}

	// --- helpers ---

	private static Set<String> idsIn(String text) {
		Set<String> ids = new TreeSet<>();
		Matcher m = PRD_ID.matcher(text);
		while (m.find()) {
			ids.add(m.group());
		}
		return ids;
	}

	private List<Path> markdownFiles() {
		return filesMatching(repoRoot.resolve("docs"), ".md", repoRoot.resolve("PROJECT_CONTEXT.md"));
	}

	private List<Path> javaFiles() {
		return filesMatching(repoRoot, ".java");
	}

	private static List<Path> filesMatching(Path dir, String suffix, Path... extra) {
		List<Path> result = new ArrayList<>();
		if (Files.isDirectory(dir)) {
			try (Stream<Path> s = Files.walk(dir)) {
				s.filter(Files::isRegularFile).filter(p -> !p.toString().contains("/target/"))
						.filter(p -> p.getFileName().toString().endsWith(suffix)).forEach(result::add);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		for (Path p : extra) {
			if (Files.isRegularFile(p)) {
				result.add(p);
			}
		}
		return result;
	}

	private static String read(Path p) {
		try {
			return Files.readString(p);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static Path locateRepoRoot() {
		Path dir = Paths.get("").toAbsolutePath();
		while (dir != null && !Files.isRegularFile(dir.resolve("PROJECT_CONTEXT.md"))) {
			dir = dir.getParent();
		}
		if (dir == null) {
			return fail("Could not locate repository root (no PROJECT_CONTEXT.md found walking up from "
					+ Paths.get("").toAbsolutePath() + ")");
		}
		return dir;
	}
}
