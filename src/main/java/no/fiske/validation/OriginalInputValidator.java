package no.fiske.validation;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The validator as it was delivered in the course project (IS-510).
 *
 * Kept unchanged on purpose, so the review findings in README.md and in
 * {@code ReviewFindingsTest} can be reproduced. Use {@link InputValidator} instead.
 */
public class OriginalInputValidator {

    private final String INVALID_CHARACTERS_REGEX = ".*[<>/()*!+\\[\\]{}\\\\].*";

    // x == 0 = valid, x < 0 = false
    private int count = 0;

    public boolean isValidInput(String input) {
        return !input.matches(INVALID_CHARACTERS_REGEX);
    }

    public boolean isValidGeojsonFile(String path) {
        // Reset count
        count = 0;
        ObjectMapper objectMapper = new ObjectMapper();

        // Read the JSON file and create a root JsonNode
        try {
            JsonNode rootNode = objectMapper.readTree(new File(path));
            iterateJsonNode(rootNode);
            return count >= 0;
        } catch (IOException e) {
            System.out.println(e.toString());
            return false;
        }
    }

    private void iterateJsonNode(JsonNode node) {
        if (node.isObject()) {
            // If it's an object, iterate over its fields
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode childNode = node.path(fieldName);
                processChildNode(childNode);
            }
        } else if (node.isArray()) {
            // If it's an array, iterate through its elements
            for (JsonNode arrayElement : node) {
                processChildNode(arrayElement);
            }
        }
    }

    private void processChildNode(JsonNode childNode) {
        if (!childNode.isContainerNode()) {
            if (!isValidInput(childNode.toString())) {
                count -= 1;
            }
        } else {
            iterateJsonNode(childNode); // Recursive call
        }
    }
}
