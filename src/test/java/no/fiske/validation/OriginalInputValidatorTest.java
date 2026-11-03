package no.fiske.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The original JUnit 5 tests from the course project.
 * Only change: hard-coded absolute paths replaced with test resources, so the
 * tests can run on any machine and in CI.
 */
class OriginalInputValidatorTest {

    private OriginalInputValidator inputValidator;

    @BeforeEach
    void setUp() {
        inputValidator = new OriginalInputValidator();
    }

    static String resource(String name) {
        try {
            Path p = Paths.get(OriginalInputValidatorTest.class.getResource("/geojson/" + name).toURI());
            return p.toString();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @DisplayName("Special characters should not be allowed.")
    void testSpecialCharactersAsInput() {
        String allowedInput = "abcdefghijklmnopqrstuvwxyzæøå .1234567890 ABCDEFGHIJKLMNOPQRSTUVWXYZÆØÅ";
        String[] notAllowedInput = {"testing <>", "??testing <>", "\\testing <>", "te%%sting <>", "test !!"};

        assertTrue(inputValidator.isValidInput(allowedInput));
        for (String input : notAllowedInput) {
            assertFalse(inputValidator.isValidInput(input));
        }
    }

    @Test
    @DisplayName("Special characters should not be allowed in GeoJSON file.")
    void testSpecialCharactersAsInputInGeoJsonFile() {
        assertFalse(inputValidator.isValidGeojsonFile(resource("invalid_script_in_value.geojson")));
        assertFalse(inputValidator.isValidGeojsonFile(resource("invalid_html_in_value.geojson")));
        assertFalse(inputValidator.isValidGeojsonFile(resource("invalid_nested_value.geojson")));

        assertTrue(inputValidator.isValidGeojsonFile(resource("valid.geojson")));
        assertTrue(inputValidator.isValidGeojsonFile(resource("valid2.geojson")));
    }
}
