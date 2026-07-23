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
    private File rootDir;
    private final Map<String, String> fileContents = new HashMap<>();
    private Map<String, Object> gameIdentity = new HashMap<>();

    private double gravity = 9.81;
    private double bounceDamping = 0.4;
    private double dangerTimeSeconds = 3.0;
    private long maxSpawnValue = 256;

    private final List<ObjectConfig> objectConfigs = new ArrayList<>();
    private final Map<String, Map<String, Object>> achievements = new LinkedHashMap<>();
    private Map<String, Object> settingsMap = new HashMap<>();
    private Map<String, Object> themeMap = new HashMap<>();
    private Map<String, Object> definesMap = new HashMap<>();

    private final Map<String, Map<String, Object>> localeCache = new HashMap<>();

    private int[] spawnWeights = {30, 27, 20, 15, 8};
    private int maxSpawnRepeat = 2;
    private int maxSpawnLevel = 5;

    private double creditsCycleSeconds = 4.0;
    private double introFallbackSeconds = 3.0;
    private double toastDuration = 3.0;
    private double comboWindow = 2.0;
    private double shakeDecay = 0.88;

    private double tauntChance = 0.25;
    private double idleTimeout = 15.0;
    private int repeatThreshold = 3;
    private boolean gameoverTaunt = true;

    private double defaultGravity = 9.81;
    private double defaultBounce = 0.4;
    private double defaultFriction = 0.97;
    private double defaultAirDrag = 0.999;
    private int defaultSubSteps = 8;
    private double gravityScale = 85.0;

    public void loadData(File rootDir) {
        this.rootDir = rootDir;
        File dataBin = new File(rootDir, "data.bin");
        if (dataBin.exists() && dataBin.length() > 0) {
            try {
                loadFromBinary(dataBin);
                isProductionMode = true;
                GameLogger.get().info("DataManager", "Loaded from data.bin");
            } catch (Exception e) {
                GameLogger.get().error("DataManager", "data.bin failed, falling back to .toml", e);
                loadFromTomlFiles(rootDir);
                isProductionMode = false;
            }
        } else {
            loadFromTomlFiles(rootDir);
            isProductionMode = false;
            GameLogger.get().info("DataManager", "Loaded from .toml files");
        }

        parseAllConfigs();
    }

    private void loadFromBinary(File dataBin) throws Exception {
        byte[] bytes = Files.readAllBytes(dataBin.toPath());
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes));

        byte[] magic = new byte[4];
        dis.readFully(magic);
        if (!Arrays.equals(magic, MAGIC_HEADER)) {
            throw new IllegalArgumentException("Invalid magic header");
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
        String[] relativePaths = {
            "version.toml",
            "game_identity.toml",
            "common/objects.toml",
            "common/pyhsics.toml",
            "common/achievements.toml",
            "common/defines.toml",
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
                    GameLogger.get().error("DataManager", "Error reading " + relPath, e);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void parseAllConfigs() {
<<<<<<< Updated upstream
=======
        // 0. Game Identity
        String identityToml = fileContents.getOrDefault("game_identity.toml", "");
        if (!identityToml.isEmpty()) {
            gameIdentity = TomlParser.parse(identityToml);
        } else {
            gameIdentity.put("title", "Project A");
            gameIdentity.put("version", "1.0.0");
            gameIdentity.put("enable_next_ball_panel", true);
        }
        // 1. Objects & Physics Settings
>>>>>>> Stashed changes
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
                double radius = 22.0 + (i - 1) * 4.6;
                objectConfigs.add(new ObjectConfig(id, i, nameKey, val, mass, score, col, radius));
            }
        }

        settingsMap = TomlParser.parse(fileContents.getOrDefault("config/settings.toml", ""));
        themeMap = TomlParser.parse(fileContents.getOrDefault("config/ui_theme.toml", ""));

        Map<String, Object> achMap = TomlParser.parse(fileContents.getOrDefault("common/achievements.toml", ""));
        Object achs = achMap.get("achievements");
        if (achs instanceof Map) {
            achievements.putAll((Map<String, Map<String, Object>>) achs);
        }

        parseDefines();
    }

    @SuppressWarnings("unchecked")
    private void parseDefines() {
        String defToml = fileContents.getOrDefault("common/defines.toml", "");
        if (defToml.isEmpty()) return;
        definesMap = TomlParser.parse(defToml);

        String weightsStr = TomlParser.getString(definesMap, "spawn.weights", "30,27,20,15,8");
        String[] wParts = weightsStr.split(",");
        spawnWeights = new int[wParts.length];
        for (int i = 0; i < wParts.length; i++) {
            spawnWeights[i] = Integer.parseInt(wParts[i].trim());
        }
        maxSpawnRepeat = (int) TomlParser.getLong(definesMap, "spawn.max_repeat", 2);
        maxSpawnLevel = (int) TomlParser.getLong(definesMap, "spawn.max_level", 5);

        creditsCycleSeconds = TomlParser.getDouble(definesMap, "ui.credits_cycle_seconds", 4.0);
        introFallbackSeconds = TomlParser.getDouble(definesMap, "ui.intro_fallback_seconds", 3.0);
        toastDuration = TomlParser.getDouble(definesMap, "ui.toast_duration", 3.0);
        comboWindow = TomlParser.getDouble(definesMap, "ui.combo_window", 2.0);
        shakeDecay = TomlParser.getDouble(definesMap, "ui.shake_decay", 0.88);

        tauntChance = TomlParser.getDouble(definesMap, "cringe.taunt_chance", 0.25);
        idleTimeout = TomlParser.getDouble(definesMap, "cringe.idle_timeout", 15.0);
        repeatThreshold = (int) TomlParser.getLong(definesMap, "cringe.repeat_threshold", 3);
        gameoverTaunt = TomlParser.getBoolean(definesMap, "cringe.gameover_taunt", true);

        defaultGravity = TomlParser.getDouble(definesMap, "physics_defaults.gravity", 9.81);
        defaultBounce = TomlParser.getDouble(definesMap, "physics_defaults.bounce", 0.4);
        defaultFriction = TomlParser.getDouble(definesMap, "physics_defaults.friction", 0.97);
        defaultAirDrag = TomlParser.getDouble(definesMap, "physics_defaults.air_drag", 0.999);
        defaultSubSteps = (int) TomlParser.getLong(definesMap, "physics_defaults.sub_steps", 8);
        gravityScale = TomlParser.getDouble(definesMap, "physics_defaults.gravity_scale", 85.0);

        dangerTimeSeconds = TomlParser.getDouble(definesMap, "danger.max_time", dangerTimeSeconds);
    }

    public String getLocalizedString(String lang, String category, String key) {
        String cacheKey = lang + "." + category + "." + key;
        String path = "locales/" + lang + ".toml";

        Map<String, Object> parsed = localeCache.get(path);
        if (parsed == null) {
            String content = fileContents.get(path);
            if (content == null && !lang.equals("tr")) {
                content = fileContents.get("locales/tr.toml");
                path = "locales/tr.toml";
            }
            if (content != null) {
                parsed = TomlParser.parse(content);
                localeCache.put(path, parsed);
            }
        }

        if (parsed != null) {
            String fullKey = lang + "." + category + "." + key;
            String val = TomlParser.getString(parsed, fullKey, null);
            if (val != null) return val;
            val = TomlParser.getString(parsed, category + "." + key, null);
            if (val != null) return val;
        }
        return key;
    }

    public List<String> getAllLocalizedKeys(String lang, String category) {
        String path = "locales/" + lang + ".toml";
        Map<String, Object> parsed = localeCache.get(path);
        if (parsed == null) {
            String content = fileContents.get(path);
            if (content != null) {
                parsed = TomlParser.parse(content);
                localeCache.put(path, parsed);
            }
        }
        List<String> keys = new ArrayList<>();
        if (parsed != null) {
            Object section = TomlParser.getNestedValue(parsed, lang + "." + category);
            if (section instanceof Map) {
                keys.addAll(((Map<String, Object>) section).keySet());
            }
        }
        return keys;
    }

    private Color parseRgbColor(String rgbStr) {
        try {
            String[] parts = rgbStr.split(",");
            if (parts.length == 3) {
                return new Color(
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim())
                );
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

    public File getRootDir() { return rootDir; }
    public boolean isProductionMode() { return isProductionMode; }
    public Map<String, Object> getGameIdentity() { return gameIdentity; }
    public double getGravity() { return gravity; }
    public double getBounceDamping() { return bounceDamping; }
    public double getDangerTimeSeconds() { return dangerTimeSeconds; }
    public long getMaxSpawnValue() { return maxSpawnValue; }
    public List<ObjectConfig> getObjectConfigs() { return objectConfigs; }
    public Map<String, Object> getSettingsMap() { return settingsMap; }
    public Map<String, Object> getThemeMap() { return themeMap; }
    public Map<String, Map<String, Object>> getAchievements() { return achievements; }

    public int[] getSpawnWeights() { return spawnWeights; }
    public int getMaxSpawnRepeat() { return maxSpawnRepeat; }
    public int getMaxSpawnLevel() { return maxSpawnLevel; }
    public double getCreditsCycleSeconds() { return creditsCycleSeconds; }
    public double getIntroFallbackSeconds() { return introFallbackSeconds; }
    public double getToastDuration() { return toastDuration; }
    public double getComboWindow() { return comboWindow; }
    public double getShakeDecay() { return shakeDecay; }
    public double getTauntChance() { return tauntChance; }
    public double getIdleTimeout() { return idleTimeout; }
    public int getRepeatThreshold() { return repeatThreshold; }
    public boolean isGameoverTaunt() { return gameoverTaunt; }
    public double getDefaultGravity() { return defaultGravity; }
    public double getDefaultBounce() { return defaultBounce; }
    public double getDefaultFriction() { return defaultFriction; }
    public double getDefaultAirDrag() { return defaultAirDrag; }
    public int getDefaultSubSteps() { return defaultSubSteps; }
    public double getGravityScale() { return gravityScale; }

    public ObjectConfig getObjectConfigByLevel(int level) {
        if (level >= 1 && level <= objectConfigs.size()) {
            return objectConfigs.get(level - 1);
        }
        return objectConfigs.get(objectConfigs.size() - 1);
    }
}
