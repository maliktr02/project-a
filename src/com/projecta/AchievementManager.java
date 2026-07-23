package com.projecta;

import java.awt.*;
import java.util.*;
import java.util.List;

public class AchievementManager {

    public static class Toast {
        public String title;
        public String description;
        public double life = 3.0; // 3 seconds on screen

        public Toast(String title, String description) {
            this.title = title;
            this.description = description;
        }
    }

    private final DataManager dataManager;
    private final SaveManager saveManager;
    private final Set<String> unlockedIds = new HashSet<>();
    private final List<Toast> activeToasts = new ArrayList<>();

    public AchievementManager(DataManager dataManager, SaveManager saveManager) {
        this.dataManager = dataManager;
        this.saveManager = saveManager;
        this.unlockedIds.addAll(saveManager.getUnlockedAchievements());
    }

    public void checkMergeAchievements(int mergedLevel, int comboCount, long totalScore, boolean escapedDanger, AudioEngine audio) {
        // 1. First Merge
        unlock("first_merge", audio);

        // 2. Reach 256
        if (mergedLevel >= 9) { // object_9 is 256
            unlock("reach_256", audio);
        }

        // 3. Reach 2048
        if (mergedLevel >= 12) { // object_12 is 2048
            unlock("reach_2048", audio);
        }
        
        // Reach 1024
        if (mergedLevel >= 11) {
            unlock("reach_1024", audio);
        }

        // 4. Combo x3
        if (comboCount >= 3) {
            unlock("combo_x3", audio);
        }
        
        // Combo x5
        if (comboCount >= 5) {
            unlock("combo_x5", audio);
        }

        // 5. Score 10k
        if (totalScore >= 10000) {
            unlock("score_10k", audio);
        }

        // 6. Danger escape
        if (escapedDanger) {
            unlock("danger_escape", audio);
        }
        
        // Check game count / drops
        if (saveManager.getTotalDrops() >= 100) {
            unlock("drop_100", audio);
        }
        if (saveManager.getTotalGames() >= 10) {
            unlock("play_10_games", audio);
        }
    }

    private void unlock(String id, AudioEngine audio) {
        if (unlockedIds.contains(id)) return;

        Map<String, Map<String, Object>> achs = dataManager.getAchievements();
        Map<String, Object> entry = achs.get(id);
        if (entry != null) {
            unlockedIds.add(id);
            saveManager.unlockAchievement(id);
            String title = (String) entry.getOrDefault("name_tr", id);
            String desc = (String) entry.getOrDefault("desc_tr", "");
            activeToasts.add(new Toast("🏆 BAŞARIM KAZANILDI: " + title, desc));

            if (audio != null) {
                audio.playComboSound(5);
            }
        }
    }

    public void update(double dt) {
        Iterator<Toast> it = activeToasts.iterator();
        while (it.hasNext()) {
            Toast t = it.next();
            t.life -= dt;
            if (t.life <= 0) it.remove();
        }
    }

    public void drawToasts(Graphics2D g2d, int screenWidth) {
        int y = 20;
        for (Toast t : activeToasts) {
            int cardW = 320;
            int cardH = 60;
            int x = screenWidth - cardW - 20;

            // Toast background card
            g2d.setColor(new Color(30, 35, 55, 230));
            g2d.fillRoundRect(x, y, cardW, cardH, 12, 12);
            g2d.setColor(new Color(255, 215, 0)); // Gold border
            g2d.setStroke(new BasicStroke(2.0f));
            g2d.drawRoundRect(x, y, cardW, cardH, 12, 12);

            // Toast text
            g2d.setFont(new Font("SansSerif", Font.BOLD, 14));
            g2d.setColor(new Color(255, 215, 0));
            g2d.drawString(t.title, x + 15, y + 25);

            g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2d.setColor(Color.WHITE);
            g2d.drawString(t.description, x + 15, y + 45);

            y += cardH + 10;
        }
    }

    public Set<String> getUnlockedIds() { return unlockedIds; }
}
