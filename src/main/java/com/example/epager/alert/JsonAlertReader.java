package com.example.epager.alert;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

final class JsonAlertReader {

    private JsonAlertReader() {
    }

    static JsonNode first(JsonNode node, String fieldName) {
        JsonNode values = node.path(fieldName);
        return values.isArray() && !values.isEmpty() ? values.get(0) : null;
    }

    static String text(JsonNode node, String... path) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        JsonNode current = node;
        for (String segment : path) {
            current = current.path(segment);
            if (current.isMissingNode() || current.isNull()) {
                return null;
            }
        }

        return current.isTextual() || current.isNumber() || current.isBoolean()
                ? current.asText()
                : null;
    }

    static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    static String generatedId() {
        return UUID.randomUUID().toString();
    }
}
