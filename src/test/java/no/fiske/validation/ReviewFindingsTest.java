package no.fiske.validation;

import static no.fiske.validation.OriginalInputValidatorTest.resource;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Findings from reviewing the original validator after the project.
 *
 * Each test shows the weakness in {@link OriginalInputValidator} (the input is
 * wrongly accepted) and that {@link InputValidator} fixes it (the input is rejected).
 */
class ReviewFindingsTest {

    private final OriginalInputValidator original = new OriginalInputValidator();
    private final InputValidator improved = new InputValidator();

    @Test
    @DisplayName("F1: A line break lets dangerous characters bypass the denylist")
    void newlineBypass() {
        // String.matches() needs the WHOLE string to match ".*[...].*", and "." does not
        // match a line break. So the regex fails to match and the input is treated as valid.
        String payload = "Mast\n<script>alert(1)</script>";

        assertTrue(original.isValidInput(payload), "original wrongly accepts the payload");
        assertFalse(improved.isValidInput(payload));
    }

    @Test
    @DisplayName("F2: Quotes and HTML entities are not on the denylist")
    void attributeInjection() {
        String payload = "x\" onmouseover=\"alert&#40;1&#41;";

        assertTrue(original.isValidInput(payload), "original wrongly accepts the payload");
        assertFalse(improved.isValidInput(payload));
    }

    @Test
    @DisplayName("F3: Property names in GeoJSON are never checked")
    void keyInjection() {
        String file = resource("invalid_key_injection.geojson");

        assertTrue(original.isValidGeojsonFile(file), "original wrongly accepts the file");
        assertFalse(improved.isValidGeojsonFile(Path.of(file)));
    }

    @Test
    @DisplayName("F4: The original tests gave false confidence - every bad case also contained <>")
    void weakTestCases() {
        // "te%%sting <>" was only rejected because of <>. Without it, % is accepted.
        assertTrue(original.isValidInput("te%%sting"), "original accepts % and ?");
        assertTrue(original.isValidInput("??testing"), "original accepts % and ?");

        assertFalse(improved.isValidInput("te%%sting"));
        assertFalse(improved.isValidInput("??testing"));
    }

    @Test
    @DisplayName("F5: Any JSON is accepted, even when it is not GeoJSON")
    void notGeoJson() {
        String file = resource("invalid_not_geojson.json");

        assertTrue(original.isValidGeojsonFile(file), "original wrongly accepts the file");
        assertFalse(improved.isValidGeojsonFile(Path.of(file)));
    }
}
