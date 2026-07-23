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
        File versionFile = new File(rootDir, "version.toml");
        if (versionFile.exists()) {
            try {
                fileContents.put("version.toml", new String(Files.readAllBytes(versionFile.toPath()), StandardCharsets.UTF_8));
            } catch (IOException ignored) {}
        }

        String[] dirsToScan = new String[]{"common", "config", "locales"};
        for (String dirName : dirsToScan) {
            File dir = new File(rootDir, dirName);
            if (dir.exists() && dir.isDirectory()) {
                scanTomlDir(dir, dirName + "/");
            }
        }
    }

    private void scanTomlDir(File dir, String prefix) {
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File f : children) {
            if (f.isDirectory()) {
                scanTomlDir(f, prefix + f.getName() + "/");
            } else if (f.getName().endsWith(".toml")) {
                try {
                    String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                    fileContents.put(prefix + f.getName(), content);
                } catch (IOException e) {
                    GameLogger.get().error("DataManager", "Error reading " + f.getName(), e);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void parseAllConfigs() {
        // Combine all common configs
        Map<String, Object> commonMap = new HashMap<>();
        for (Map.Entry<String, String> entry : fileContents.entrySet()) {
            if (entry.getKey().startsWith("common/")) {
                Map<String, Object> parsed = TomlParser.parse(entry.getValue());
                deepMerge(commonMap, parsed);
            }
        }

        gravity = TomlParser.getDouble(commonMap, "_settings.gravity", 9.81);
        bounceDamping = TomlParser.getDouble(commonMap, "_settings.bounce_damping", 0.4);
        dangerTimeSeconds = TomlParser.getDouble(commonMap, "_settings.danger_time_seconds", 3.0);
        maxSpawnValue = TomlParser.getLong(commonMap, "_settings.max_spawn_value", 256);

        objectConfigs.clear();
        for (int i = 1; i <= 12; i++) {
            String key = "objects.object_" + i;
            if (TomlParser.getNestedValue(commonMap, key) != null) {
                String id = TomlParser.getString(commonMap, key + ".id", "object_" + i);
                String nameKey = TomlParser.getString(commonMap, key + ".name", "object_" + i + "_name");
                long val = TomlParser.getLong(commonMap, key + ".value", (long) Math.pow(2, i - 1));
                double mass = TomlParser.getDouble(commonMap, key + ".mass", 1.0 + i * 0.7);
                long score = TomlParser.getLong(commonMap, key + ".score", val * 2);
                String colorRgb = TomlParser.getString(commonMap, key + ".color_rgb", "238, 228, 218");
                Color col = parseRgbColor(colorRgb);

                // Radius scaling: base 22px up to ~72px for level 12
                double radius = 22.0 + (i - 1) * 4.6;

                objectConfigs.add(new ObjectConfig(id, i, nameKey, val, mass, score, col, radius));
            }
        }

        // 2. Settings & UI Theme
        settingsMap = new HashMap<>();
        themeMap = new HashMap<>();
        for (Map.Entry<String, String> entry : fileContents.entrySet()) {
            if (entry.getKey().startsWith("config/")) {
                Map<String, Object> parsed = TomlParser.parse(entry.getValue());
                if (entry.getKey().contains("settings")) deepMerge(settingsMap, parsed);
                else deepMerge(themeMap, parsed);
            }
        }

        // 3. Achievements
        Object achs = commonMap.get("achievements");
        if (achs instanceof Map) {
            achievements.putAll((Map<String, Map<String, Object>>) achs);
        }
    }

    @SuppressWarnings("unchecked")
    private void deepMerge(Map<String, Object> target, Map<String, Object> source) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map && target.containsKey(key) && target.get(key) instanceof Map) {
                deepMerge((Map<String, Object>) target.get(key), (Map<String, Object>) value);
            } else {
                target.put(key, value);
            }
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
