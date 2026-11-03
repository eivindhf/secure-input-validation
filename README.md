# Secure input validation for GeoJSON uploads

Input validation for a web application where users register map objects through a form or by uploading GeoJSON files. The code comes from a course project in secure software development (IS-510, University of Agder), carried out together with a public-sector organisation.

This repo shows three things:

1. **The original solution** from the project, unchanged (`OriginalInputValidator`), with the original JUnit 5 tests.
2. **A code review of that solution**: five findings, each reproduced as a test (`ReviewFindingsTest`).
3. **An improved version** (`InputValidator`) that is checked automatically in a CI pipeline with unit tests, SAST and dependency scanning.

## Review findings

| # | Finding | Why it matters |
|---|---------|----------------|
| F1 | The denylist regex `.*[...].*` is used with `String.matches()`. `.` does not match a line break, so `"Mast\n<script>"` is accepted. | Complete bypass of the check |
| F2 | Quotes and HTML entities are not on the denylist, so `x" onmouseover="alert&#40;1&#41;` is accepted. | Attribute injection if output is not encoded |
| F3 | Only GeoJSON values are checked. Property names are not. | Injection through keys |
| F4 | Every rejected test case also contained `<>`, so the tests never showed whether `%` or `?` were blocked. They were not. | Tests gave false confidence |
| F5 | Any JSON file is accepted, even when it is not GeoJSON. | No structural validation |

Smaller issues: values were read with `JsonNode.toString()` (JSON-escaped, with quotes) instead of `asText()`. Exceptions were printed to `System.out`. A mutable counter made the class unsafe to share between threads. The tests used hard-coded absolute paths, so they could not run in CI. The TypeScript version called `.test()` on a string, so it would not have compiled (fixed in `frontend/inputValidator.ts`).

## What the improved version does

- **Allowlist instead of denylist.** It accepts letters (including æøå), digits, space and `. , - _ :`, anchored to the whole string.
- **Checks both keys and values** in GeoJSON, reading values with `asText()`.
- **Checks that the document is GeoJSON**, with a known `type`.
- **Limits** on text length (256), file size (5 MB) and nesting depth (64), to reduce the risk of denial of service.
- **Fails closed**, is stateless, and does not print internal errors.

Validation is defence in depth. The main protection against XSS is still context-aware output encoding where the data is displayed.

## CI pipeline

`.github/workflows/ci.yml` runs on every push and pull request:

| Job | Tool | Fails the build when |
|-----|------|----------------------|
| Build and unit tests | Maven + JUnit 5 | a test fails |
| SAST | Semgrep: custom rules in `.semgrep/`, plus `p/java` and `p/secrets` | a finding in production code |
| Dependency scan | Trivy | a HIGH or CRITICAL CVE in a dependency |

Dependabot keeps Maven dependencies and GitHub Actions up to date.

The custom Semgrep rules in `.semgrep/input-validation.yml` are written to catch exactly the mistakes from the review: denylist regexes wrapped in `.*`, validation of `JsonNode.toString()`, and printing errors to `System.out`. The pipeline also runs them on the original code as a report, so the findings are visible without breaking the build.

## Run locally

```bash
mvn verify                       # build and run all tests
semgrep scan --config .semgrep/  # custom SAST rules
node frontend/check.mjs          # sanity check of the front-end regex
```

Requires Java 17+ and Maven.

## Limitations and next steps

- The allowlist is strict. Legitimate names with an apostrophe (`O'Brien`) are rejected. In a real system, the allowed set should be agreed with the product owner.
- Full validation against a JSON Schema for GeoJSON (RFC 7946), including coordinate ranges.
- GML and SOSI uploads were not covered. One option is to convert them to GeoJSON first.
- Pin GitHub Actions to commit SHAs, and add CodeQL for deeper analysis.
