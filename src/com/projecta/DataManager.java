package com.projecta;

import java.awt.Color;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class DataManager {

    private static final String ENCRYPTION_KEY_PHRASE = "ProjectA_2048_Suika_Physics_Engine_SecretKey_2026";
    private static final byte[] MAGIC_HEADER = new byte[]{'P', 'R', 'O', 'J'};

    private boolean isProductionMode = false;
    private final Map<String, String> fileContents = new HashMap<>();

    private double gravity = 9.81;
    private double bounceDamping = 0.4;
    private double dangerTimeSeconds = 3.0;
    private long maxSpawnValue = 256;

    private final List<ObjectConfig> objectConfigs = new ArrayList<>();
    private final Map<String, Map<String, Object>> achievements = new LinkedHashMap<>();
    private Map<String, Object> settingsMap = new HashMap<>();
    private Map<String, Object> themeMap = new HashMap<>();

    public void loadData(File rootDir) {
        File dataBin = new File(rootDir, "data.bin");
        if (dataBin.exists() && dataBin.length() > 0) {
            try {
                loadFromBinary(dataBin);
                isProductionMode = true;
                System.out.println("[DataManager] Loaded configuration from production binary: data.bin");
            } catch (Exception e) {
                System.err.println("[DataManager] Failed to load data.bin, falling back to .toml files: " + e.getMessage());
                loadFromTomlFiles(rootDir);
                isProductionMode = false;
            }
        } else {
            loadFromTomlFiles(rootDir);
            isProductionMode = false;
            System.out.println("[DataManager] Loaded configuration from developer .toml files");
        }

        parseAllConfigs();
    }

    private void loadFromBinary(File dataBin) throws Exception {
        byte[] bytes = Files.readAllBytes(dataBin.toPath());
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes));

        byte[] magic = new byte[4];
        dis.readFully(magic);
        if (!Arrays.equals(magic, MAGIC_HEADER)) {
            throw new IllegalArgumentException("Invalid magic header in data.bin");
        }

        int fileCount = dis.readInt();
        byte[] keyBytes = deriveKey(ENCRYPTION_KEY_PHRASE);

        for (int i = 0; i < fileCount; i++) {
            short pathLen = dis.readShort();
            byte[] pathBytes = new byte[pathLen];
            dis.readFully(pathBytes);
            String path = new String(pathBytes, StandardCharsets.UTF_8);

            int encLen = dis.readInt();
            byte[] encBytes = new byte[encLen];
            dis.readFully(encBytes);

            byte[] decrypted = decryptAES(encBytes, keyBytes);
            String content = new String(decrypted, StandardCharsets.UTF_8);
            fileContents.put(path.replace('\\', '/'), content);
        }
    }

    private void loadFromTomlFiles(File rootDir) {
        String[] relativePaths = new String[]{
            "version.toml",
            "common/objects.toml",
            "common/pyhsics.toml",
            "common/achievements.toml",
            "config/settings.toml",
            "config/ui_theme.toml",
            "locales/tr.toml",
            "locales/en.toml"
        };

        for (String relPath : relativePaths) {
            File f = new File(rootDir, relPath);
            if (f.exists()) {
                try {
                    String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                    fileContents.put(relPath, content);
                } catch (IOException e) {
                    System.err.println("[DataManager] Error reading " + relPath + ": " + e.getMessage());
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void parseAllConfigs() {
        // 1. Objects & Physics Settings
        String objectsToml = fileContents.getOrDefault("common/objects.toml", "");
        Map<String, Object> parsedObjects = TomlParser.parse(objectsToml);

        gravity = TomlParser.getDouble(parsedObjects, "_settings.gravity", 9.81);
        bounceDamping = TomlParser.getDouble(parsedObjects, "_settings.bounce_damping", 0.4);
        dangerTimeSeconds = TomlParser.getDouble(parsedObjects, "_settings.danger_time_seconds", 3.0);
        maxSpawnValue = TomlParser.getLong(parsedObjects, "_settings.max_spawn_value", 256);

        objectConfigs.clear();
        for (int i = 1; i <= 12; i++) {
            String key = "objects.object_" + i;
            if (TomlParser.getNestedValue(parsedObjects, key) != null) {
                String id = TomlParser.getString(parsedObjects, key + ".id", "object_" + i);
                String nameKey = TomlParser.getString(parsedObjects, key + ".name", "object_" + i + "_name");
                long val = TomlParser.getLong(parsedObjects, key + ".value", (long) Math.pow(2, i - 1));
                double mass = TomlParser.getDouble(parsedObjects, key + ".mass", 1.0 + i * 0.7);
                long score = TomlParser.getLong(parsedObjects, key + ".score", val * 2);
                String colorRgb = TomlParser.getString(parsedObjects, key + ".color_rgb", "238, 228, 218");
                Color col = parseRgbColor(colorRgb);

                // Radius scaling: base 22px up to ~72px for level 12
                double radius = 22.0 + (i - 1) * 4.6;

                objectConfigs.add(new ObjectConfig(id, i, nameKey, val, mass, score, col, radius));
            }
        }

        // 2. Settings & UI Theme
        settingsMap = TomlParser.parse(fileContents.getOrDefault("config/settings.toml", ""));
        themeMap = TomlParser.parse(fileContents.getOrDefault("config/ui_theme.toml", ""));

        // 3. Achievements
        Map<String, Object> achMap = TomlParser.parse(fileContents.getOrDefault("common/achievements.toml", ""));
        Object achs = achMap.get("achievements");
        if (achs instanceof Map) {
            achievements.putAll((Map<String, Map<String, Object>>) achs);
        }
    }

    public String getLocalizedString(String lang, String category, String key) {
        String path = "locales/" + lang + ".toml";
        String content = fileContents.get(path);
        if (content == null && !lang.equals("tr")) {
            content = fileContents.get("locales/tr.toml");
        }
        if (content != null) {
            Map<String, Object> parsed = TomlParser.parse(content);
            String fullKey = lang + "." + category + "." + key;
            String val = TomlParser.getString(parsed, fullKey, null);
            if (val != null) return val;

            // Fallback try without lang prefix
            val = TomlParser.getString(parsed, category + "." + key, null);
            if (val != null) return val;
        }
        return key;
    }

    private Color parseRgbColor(String rgbStr) {
        try {
            String[] parts = rgbStr.split(",");
            if (parts.length == 3) {
                int r = Integer.parseInt(parts[0].trim());
                int g = Integer.parseInt(parts[1].trim());
                int b = Integer.parseInt(parts[2].trim());
                return new Color(r, g, b);
            }
        } catch (Exception ignored) {}
        return new Color(220, 220, 220);
    }

    private static byte[] deriveKey(String passphrase) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] hash = sha.digest(passphrase.getBytes(StandardCharsets.UTF_8));
        byte[] key = new byte[16];
        System.arraycopy(hash, 0, key, 0, 16);
        return key;
    }

    private static byte[] decryptAES(byte[] encryptedData, byte[] keyBytes) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        return cipher.doFinal(encryptedData);
    }

    public boolean isProductionMode() { return isProductionMode; }
    public double getGravity() { return gravity; }
    public double getBounceDamping() { return bounceDamping; }
    public double getDangerTimeSeconds() { return dangerTimeSeconds; }
    public long getMaxSpawnValue() { return maxSpawnValue; }
    public List<ObjectConfig> getObjectConfigs() { return objectConfigs; }
    public Map<String, Object> getSettingsMap() { return settingsMap; }
    public Map<String, Object> getThemeMap() { return themeMap; }
    public Map<String, Map<String, Object>> getAchievements() { return achievements; }

    public ObjectConfig getObjectConfigByLevel(int level) {
        if (level >= 1 && level <= objectConfigs.size()) {
            return objectConfigs.get(level - 1);
        }
        return objectConfigs.get(objectConfigs.size() - 1);
    }
}
