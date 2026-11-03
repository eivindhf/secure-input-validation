package no.fiske.validation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Improved validator for free-text input and GeoJSON uploads.
 *
 * Changes compared with {@link OriginalInputValidator}:
 * <ul>
 *   <li>Allowlist instead of denylist: only letters, digits and a few safe
 *       punctuation characters are accepted, anchored to the whole string,
 *       so line breaks can no longer slip past the check.</li>
 *   <li>Checks both property names and values, and reads values with
 *       {@code asText()} instead of {@code toString()}, which added JSON escaping.</li>
 *   <li>Enforces a maximum length, file size and nesting depth to limit
 *       denial of service through oversized or deeply nested files.</li>
 *   <li>Checks that the document is GeoJSON (a known {@code type}).</li>
 *   <li>Stateless and thread-safe, fails closed, and does not print errors.</li>
 * </ul>
 *
 * Validation is defence in depth. Output must still be encoded for the
 * context it is shown in (HTML, attribute, JavaScript, SQL parameters).
 */
public final class InputValidator {

    public static final int MAX_TEXT_LENGTH = 256;
    public static final long MAX_FILE_BYTES = 5L * 1024 * 1024;
    public static final int MAX_NESTING_DEPTH = 64;

    /** Letters (incl. æøå), digits, space and . , - _ : */
    private static final Pattern ALLOWED_TEXT =
            Pattern.compile("[\\p{L}\\p{N} .,\\-_:]{0," + MAX_TEXT_LENGTH + "}");

    private static final Set<String> GEOJSON_TYPES = Set.of(
            "FeatureCollection", "Feature", "Point", "MultiPoint", "LineString",
            "MultiLineString", "Polygon", "MultiPolygon", "GeometryCollection");

    private final ObjectMapper mapper;

    public InputValidator() {
        JsonFactory factory = JsonFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder()
                        .maxNestingDepth(MAX_NESTING_DEPTH)
                        .maxStringLength(MAX_TEXT_LENGTH * 4)
                        .build())
                .build();
        this.mapper = new ObjectMapper(factory);
    }

    /** Returns true only if the whole input consists of allowed characters. */
    public boolean isValidInput(String input) {
        return input != null && ALLOWED_TEXT.matcher(input).matches();
    }

    /** Validates a GeoJSON file on disk. Fails closed on any error. */
    public boolean isValidGeojsonFile(Path path) {
        try {
            if (path == null || !Files.isRegularFile(path) || Files.size(path) > MAX_FILE_BYTES) {
                return false;
            }
            try (InputStream in = Files.newInputStream(path)) {
                return isValidGeojson(in);
            }
        } catch (IOException e) {
            return false;
        }
    }

    /** Validates GeoJSON from a stream, e.g. an uploaded file. Fails closed on any error. */
    public boolean isValidGeojson(InputStream in) {
        try {
            JsonNode root = mapper.readTree(in);
            if (root == null || !root.isObject()) {
                return false;
            }
            JsonNode type = root.get("type");
            if (type == null || !type.isTextual() || !GEOJSON_TYPES.contains(type.asText())) {
                return false;
            }
            return isValidNode(root);
        } catch (IOException e) {
            return false;
        }
    }

    private boolean isValidNode(JsonNode node) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (!isValidInput(field.getKey()) || !isValidNode(field.getValue())) {
                    return false;
                }
            }
            return true;
        }
        if (node.isArray()) {
            for (JsonNode element : node) {
                if (!isValidNode(element)) {
                    return false;
                }
            }
            return true;
        }
        if (node.isTextual()) {
            return isValidInput(node.asText());
        }
        // Numbers, booleans and null carry no injectable text.
        return node.isNumber() || node.isBoolean() || node.isNull();
    }
}
