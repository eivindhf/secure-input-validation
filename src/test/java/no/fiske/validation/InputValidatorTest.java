package no.fiske.validation;

import static no.fiske.validation.OriginalInputValidatorTest.resource;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class InputValidatorTest {

    private final InputValidator validator = new InputValidator();

    @Nested
    @DisplayName("Free-text input")
    class TextInput {

        @ParameterizedTest
        @ValueSource(strings = {
                "Mast ved Lillesand",
                "Kraftledning Ålesund-Ørsta",
                "Tårn 42.5 m, kategori B_2",
                "abcdefghijklmnopqrstuvwxyzæøå 1234567890 ABCDEFGHIJKLMNOPQRSTUVWXYZÆØÅ"
        })
        void acceptsNormalNames(String input) {
            assertTrue(validator.isValidInput(input));
        }

        @ParameterizedTest
        @ValueSource(strings = {"<", ">", "/", "(", ")", "*", "!", "+", "[", "]", "{", "}", "\\",
                "\"", "'", "`", ";", "&", "#", "%", "=", "?", "$", "|", "@"})
        void rejectsEachSpecialCharacterOnItsOwn(String c) {
            // Each character is tested alone, so one bad character cannot hide behind another.
            assertFalse(validator.isValidInput("Mast" + c + "navn"), "should reject: " + c);
        }

        @ParameterizedTest
        @ValueSource(strings = {"Mast\nnavn", "Mast\rnavn", "Mast\tnavn", "Mast\u0000navn"})
        void rejectsControlCharacters(String input) {
            assertFalse(validator.isValidInput(input));
        }

        @Test
        void rejectsNull() {
            assertFalse(validator.isValidInput(null));
        }

        @ParameterizedTest
        @NullAndEmptySource
        void emptyStringIsAllowedButNullIsNot(String input) {
            // Required fields are a business rule, checked elsewhere.
            if (input == null) {
                assertFalse(validator.isValidInput(input));
            } else {
                assertTrue(validator.isValidInput(input));
            }
        }

        @Test
        void rejectsTooLongInput() {
            String ok = "a".repeat(InputValidator.MAX_TEXT_LENGTH);
            assertTrue(validator.isValidInput(ok));
            assertFalse(validator.isValidInput(ok + "a"));
        }
    }

    @Nested
    @DisplayName("GeoJSON files")
    class GeoJsonFiles {

        @ParameterizedTest
        @ValueSource(strings = {"valid.geojson", "valid2.geojson"})
        void acceptsValidFiles(String name) {
            assertTrue(validator.isValidGeojsonFile(Path.of(resource(name))));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "invalid_script_in_value.geojson",
                "invalid_html_in_value.geojson",
                "invalid_nested_value.geojson",
                "invalid_key_injection.geojson",
                "invalid_attribute_injection.geojson",
                "invalid_not_geojson.json"
        })
        void rejectsInvalidFiles(String name) {
            assertFalse(validator.isValidGeojsonFile(Path.of(resource(name))));
        }

        @Test
        void rejectsMissingFile() {
            assertFalse(validator.isValidGeojsonFile(Path.of("does-not-exist.geojson")));
            assertFalse(validator.isValidGeojsonFile(null));
        }

        @Test
        void rejectsMalformedJson() {
            assertFalse(validate("{ \"type\": \"Feature\", "));
            assertFalse(validate("not json at all"));
            assertFalse(validate("[]"));
        }

        @Test
        @DisplayName("Rejects deeply nested JSON (denial of service)")
        void rejectsDeepNesting() {
            String deep = "{\"type\":\"Feature\",\"properties\":{\"a\":"
                    + "[".repeat(200) + "]".repeat(200) + "}}";
            assertFalse(validate(deep));
        }

        @Test
        void acceptsFromStream() {
            assertTrue(validate("{\"type\":\"Point\",\"coordinates\":[8.0,58.0]}"));
        }

        private boolean validate(String json) {
            return validator.isValidGeojson(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        }
    }
}
