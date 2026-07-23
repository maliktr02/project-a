package com.projecta;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class SaveManager {

    private static final String SAVE_FILE = "save_data.dat";
    private static final String SETTINGS_FILE = "user_settings.dat";
    private static final String SAVE_SECRET = "ProjectA_SaveData_Secret_2026";

    private final File saveDir;
    private final Properties saveData = new Properties();
    private final Properties settingsData = new Properties();

    public SaveManager(File rootDir) {
        this.saveDir = new File(rootDir, "userdata");
        if (!saveDir.exists()) saveDir.mkdirs();
        loadAll();
    }

    private void loadAll() {
        if (new File(saveDir, SAVE_FILE).exists()) {
            loadEncryptedProps(new File(saveDir, SAVE_FILE), saveData);
        }
        loadProps(new File(saveDir, SETTINGS_FILE), settingsData);
    }

    private void loadProps(File f, Properties props) {
        if (f.exists()) {
            try (InputStream is = new FileInputStream(f)) {
                props.load(is);
            } catch (IOException e) {
                GameLogger.get().error("SaveManager", "Failed to load " + f.getName(), e);
            }
        }
    }

    private void saveProps(File f, Properties props) {
        try (OutputStream os = new FileOutputStream(f)) {
            props.store(os, "Project A Settings");
        } catch (IOException e) {
            GameLogger.get().error("SaveManager", "Failed to save " + f.getName(), e);
        }
    }

    private void loadEncryptedProps(File f, Properties props) {
        try {
            byte[] keyBytes = deriveKey(SAVE_SECRET);
            byte[] encBytes = java.nio.file.Files.readAllBytes(f.toPath());
            byte[] decrypted = decryptAES(encBytes, keyBytes);
            props.load(new ByteArrayInputStream(decrypted));
        } catch (Exception e) {
            GameLogger.get().error("SaveManager", "Failed to load/decrypt " + f.getName() + ". Using empty save.", e);
        }
    }

    private void saveEncryptedProps(File f, Properties props) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            props.store(baos, "Project A Encrypted Save Data");
            byte[] keyBytes = deriveKey(SAVE_SECRET);
            byte[] encrypted = encryptAES(baos.toByteArray(), keyBytes);
            java.nio.file.Files.write(f.toPath(), encrypted);
        } catch (Exception e) {
            GameLogger.get().error("SaveManager", "Failed to encrypt/save " + f.getName(), e);
        }
    }

    private static byte[] deriveKey(String passphrase) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] hash = sha.digest(passphrase.getBytes(StandardCharsets.UTF_8));
        return Arrays.copyOf(hash, 16);
    }

    private static byte[] encryptAES(byte[] data, byte[] keyBytes) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        return cipher.doFinal(data);
    }

    private static byte[] decryptAES(byte[] encryptedData, byte[] keyBytes) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        return cipher.doFinal(encryptedData);
    }

    public long getHighScore() {
        return Long.parseLong(saveData.getProperty("high_score", "0"));
    }

    public void setHighScore(long score) {
        if (score > getHighScore()) {
            saveData.setProperty("high_score", String.valueOf(score));
            savePersistentData();
        }
    }

    public int getTotalGames() {
        return Integer.parseInt(saveData.getProperty("total_games", "0"));
    }

    public void incrementTotalGames() {
        saveData.setProperty("total_games", String.valueOf(getTotalGames() + 1));
        savePersistentData();
    }

    public int getHighestLevel() {
        return Integer.parseInt(saveData.getProperty("highest_level", "0"));
    }

    public void setHighestLevel(int level) {
        if (level > getHighestLevel()) {
            saveData.setProperty("highest_level", String.valueOf(level));
            savePersistentData();
        }
    }

    public long getTotalMerges() {
        return Long.parseLong(saveData.getProperty("total_merges", "0"));
    }

    public void addMerges(int count) {
        saveData.setProperty("total_merges", String.valueOf(getTotalMerges() + count));
        savePersistentData();
    }

    public long getTotalDrops() {
        return Long.parseLong(saveData.getProperty("total_drops", "0"));
    }

    public void addDrop() {
        saveData.setProperty("total_drops", String.valueOf(getTotalDrops() + 1));
        savePersistentData();
    }

    public int getMaxCombo() {
        return Integer.parseInt(saveData.getProperty("max_combo", "0"));
    }

    public void setMaxCombo(int combo) {
        if (combo > getMaxCombo()) {
            saveData.setProperty("max_combo", String.valueOf(combo));
            savePersistentData();
        }
    }

    public Set<String> getUnlockedAchievements() {
        String raw = saveData.getProperty("achievements", "");
        if (raw.isEmpty()) return new HashSet<>();
        return new HashSet<>(Arrays.asList(raw.split(",")));
    }

    public void unlockAchievement(String id) {
        Set<String> set = getUnlockedAchievements();
        set.add(id);
        saveData.setProperty("achievements", String.join(",", set));
        savePersistentData();
    }

    public String getSetting(String key, String defaultVal) {
        return settingsData.getProperty(key, defaultVal);
    }

    public int getSettingInt(String key, int defaultVal) {
        try { return Integer.parseInt(settingsData.getProperty(key, String.valueOf(defaultVal))); }
        catch (NumberFormatException e) { return defaultVal; }
    }

    public boolean getSettingBool(String key, boolean defaultVal) {
        return Boolean.parseBoolean(settingsData.getProperty(key, String.valueOf(defaultVal)));
    }

    public void setSetting(String key, String value) {
        settingsData.setProperty(key, value);
        saveSettings();
    }

    public void setSetting(String key, int value) { setSetting(key, String.valueOf(value)); }
    public void setSetting(String key, boolean value) { setSetting(key, String.valueOf(value)); }

    private void savePersistentData() {
        saveEncryptedProps(new File(saveDir, SAVE_FILE), saveData);
    }

    private void saveSettings() {
        saveProps(new File(saveDir, SETTINGS_FILE), settingsData);
    }
}
