package com.example.coursework.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

public class FlattenUtils {
    public static Map<String, String> flatten(JsonNode node, String parentPath) {
        Map<String, String> flatMap = new LinkedHashMap<>();
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String fieldName = entry.getKey();
                JsonNode value = entry.getValue();
                String path = parentPath.isEmpty() ? fieldName : parentPath + "." + fieldName;
                flatMap.putAll(flatten(value, path));
            });
        } else if (node.isArray()) {
            // Упрощение: если массив, преобразуем его в строку JSON (не разворачиваем)
            flatMap.put(parentPath, node.toString());
        } else if (!node.isNull()) {
            flatMap.put(parentPath, node.asText());
        }
        return flatMap;
    }

    public static JsonNode expand(Map<String, String> flatData, ObjectMapper mapper) {
        Map<String, Object> root = new LinkedHashMap<>();
        flatData.forEach((path, value) -> {
            String[] parts = path.split("\\.");
            Map<String, Object> current = root;
            for (int i = 0; i < parts.length - 1; i++) {
                String part = parts[i];
                current = (Map<String, Object>) current.computeIfAbsent(part, k -> new LinkedHashMap<>());
            }
            String lastPart = parts[parts.length - 1];
            // Попытка распарсить число или булево (упрощённо)
            if (value.matches("-?\\d+")) {
                current.put(lastPart, Long.parseLong(value));
            } else if (value.matches("-?\\d+\\.\\d+")) {
                current.put(lastPart, Double.parseDouble(value));
            } else if (value.equals("true") || value.equals("false")) {
                current.put(lastPart, Boolean.parseBoolean(value));
            } else {
                current.put(lastPart, value);
            }
        });
        return mapper.valueToTree(root);
    }
}