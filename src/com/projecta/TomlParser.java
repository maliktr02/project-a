package com.projecta;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.*;

public class TomlParser {

    public static Map<String, Object> parse(String content) {
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> currentSection = root;

        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Ignore empty lines and comment lines
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Strip inline comments if not inside quotes
                line = stripComment(line);
                if (line.isEmpty()) continue;

                // Section header [section] or [section.subsection]
                if (line.startsWith("[") && line.endsWith("]")) {
                    String sectionName = line.substring(1, line.length() - 1).trim();
                    currentSection = getOrCreateSection(root, sectionName);
                    continue;
                }

                // Key-value pair
                int equalsIdx = line.indexOf('=');
                if (equalsIdx > 0) {
                    String key = line.substring(0, equalsIdx).trim();
                    String valStr = line.substring(equalsIdx + 1).trim();
                    Object value = parseValue(valStr);
                    currentSection.put(key, value);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return root;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getOrCreateSection(Map<String, Object> root, String sectionPath) {
        String[] parts = sectionPath.split("\\.");
        Map<String, Object> current = root;

        for (String part : parts) {
            Object obj = current.get(part);
            if (obj instanceof Map) {
                current = (Map<String, Object>) obj;
            } else {
                Map<String, Object> newSection = new LinkedHashMap<>();
                current.put(part, newSection);
                current = newSection;
            }
        }
        return current;
    }

    private static String stripComment(String line) {
        boolean inQuotes = false;
        char quoteChar = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if ((c == '"' || c == '\'') && (i == 0 || line.charAt(i - 1) != '\\')) {
                if (!inQuotes) {
                    inQuotes = true;
                    quoteChar = c;
                } else if (quoteChar == c) {
                    inQuotes = false;
                }
            } else if (c == '#' && !inQuotes) {
                return line.substring(0, i).trim();
            }
        }
        return line.trim();
    }

    private static Object parseValue(String str) {
        if (str.startsWith("\"") && str.endsWith("\"") && str.length() >= 2) {
            return str.substring(1, str.length() - 1);
        }
        if (str.startsWith("'") && str.endsWith("'") && str.length() >= 2) {
            return str.substring(1, str.length() - 1);
        }
        if (str.equalsIgnoreCase("true")) return Boolean.TRUE;
        if (str.equalsIgnoreCase("false")) return Boolean.FALSE;

        try {
            if (str.contains(".")) {
                return Double.parseDouble(str);
            } else {
                return Long.parseLong(str);
            }
        } catch (NumberFormatException ignored) {}

        return str;
    }

    @SuppressWarnings("unchecked")
    public static String getString(Map<String, Object> map, String path, String defaultVal) {
        Object val = getNestedValue(map, path);
        return val != null ? val.toString() : defaultVal;
    }

    @SuppressWarnings("unchecked")
    public static double getDouble(Map<String, Object> map, String path, double defaultVal) {
        Object val = getNestedValue(map, path);
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        if (val != null) {
            try {
                return Double.parseDouble(val.toString());
            } catch (Exception ignored) {}
        }
        return defaultVal;
    }

    @SuppressWarnings("unchecked")
    public static long getLong(Map<String, Object> map, String path, long defaultVal) {
        Object val = getNestedValue(map, path);
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        if (val != null) {
            try {
                return Long.parseLong(val.toString());
            } catch (Exception ignored) {}
        }
        return defaultVal;
    }

    @SuppressWarnings("unchecked")
    public static boolean getBoolean(Map<String, Object> map, String path, boolean defaultVal) {
        Object val = getNestedValue(map, path);
        if (val instanceof Boolean) return (Boolean) val;
        if (val != null) return Boolean.parseBoolean(val.toString());
        return defaultVal;
    }

    @SuppressWarnings("unchecked")
    public static Object getNestedValue(Map<String, Object> root, String path) {
        String[] parts = path.split("\\.");
        Object current = root;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else {
                return null;
            }
        }
        return current;
    }
}
