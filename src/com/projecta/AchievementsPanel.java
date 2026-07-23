package com.projecta;

import java.awt.*;
import java.util.Map;
import java.util.Set;

public class AchievementsPanel {

    public interface AchievementsListener {
        void onBack();
    }

    private final DataManager dataManager;
    private final AchievementManager achievementManager;
    private final AchievementsListener listener;
    private String currentLang = "tr";

    private Rectangle backButtonRect;
    private boolean isBackHovered = false;

    public AchievementsPanel(DataManager dataManager, AchievementManager achievementManager, String currentLang, AchievementsListener listener) {
        this.dataManager = dataManager;
        this.achievementManager = achievementManager;
        this.currentLang = currentLang;
        this.listener = listener;
    }

    public void setLanguage(String lang) {
        this.currentLang = lang;
    }

    public void mouseMoved(Point p) {
        if (backButtonRect != null) {
            isBackHovered = backButtonRect.contains(p);
        }
    }

    public void mouseClicked(Point p) {
        if (backButtonRect != null && backButtonRect.contains(p)) {
            listener.onBack();
        }
    }

    public void draw(Graphics2D g2d, int width, int height) {
        // Background
        g2d.setColor(new Color(18, 19, 28));
        g2d.fillRect(0, 0, width, height);

        // Header Title
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 42));
        FontMetrics tfm = g2d.getFontMetrics();
        String title = dataManager.getLocalizedString(currentLang, "ui", "achievements_title");
        g2d.drawString(title, (width - tfm.stringWidth(title)) / 2, 70);

        // Achievements List Container
        int cardW = 600;
        int cardH = 460;
        int cardX = (width - cardW) / 2;
        int cardY = 100;

        g2d.setColor(new Color(46, 52, 64, 200));
        g2d.fillRoundRect(cardX, cardY, cardW, cardH, 20, 20);
        g2d.setColor(new Color(76, 86, 106));
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawRoundRect(cardX, cardY, cardW, cardH, 20, 20);

        Map<String, Map<String, Object>> achs = dataManager.getAchievements();
        Set<String> unlocked = achievementManager.getUnlockedIds();

        int itemY = cardY + 25;
        int itemH = 60;

        for (Map.Entry<String, Map<String, Object>> entry : achs.entrySet()) {
            String key = entry.getKey();
            Map<String, Object> ach = entry.getValue();

            boolean isUnlocked = unlocked.contains(key);
            String nameKey = currentLang.equals("en") ? "name_en" : "name_tr";
            String descKey = currentLang.equals("en") ? "desc_en" : "desc_tr";

            String name = (String) ach.getOrDefault(nameKey, key);
            String desc = (String) ach.getOrDefault(descKey, "");

            // Item Row Background
            int itemX = cardX + 20;
            int itemW = cardW - 40;

            g2d.setColor(isUnlocked ? new Color(59, 66, 82) : new Color(36, 41, 51));
            g2d.fillRoundRect(itemX, itemY, itemW, itemH, 10, 10);
            g2d.setColor(isUnlocked ? new Color(255, 215, 0) : new Color(76, 86, 106));
            g2d.setStroke(new BasicStroke(1.2f));
            g2d.drawRoundRect(itemX, itemY, itemW, itemH, 10, 10);

            // Icon indicator
            g2d.setFont(new Font("SansSerif", Font.BOLD, 22));
            g2d.drawString(isUnlocked ? "🏆" : "🔒", itemX + 15, itemY + 38);

            // Text
            g2d.setFont(new Font("SansSerif", Font.BOLD, 16));
            g2d.setColor(isUnlocked ? new Color(255, 215, 0) : new Color(143, 188, 187));
            g2d.drawString(name, itemX + 55, itemY + 26);

            g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2d.setColor(isUnlocked ? new Color(236, 239, 244) : new Color(129, 161, 193));
            g2d.drawString(desc, itemX + 55, itemY + 46);

            itemY += itemH + 12;
            if (itemY + itemH > cardY + cardH - 60) break; // clamp page
        }

        // Back Button
        int btnW = 180;
        int btnH = 40;
        int btnX = (width - btnW) / 2;
        int btnY = cardY + cardH - 52;
        backButtonRect = new Rectangle(btnX, btnY, btnW, btnH);

        g2d.setColor(isBackHovered ? new Color(136, 192, 208) : new Color(59, 66, 82));
        g2d.fillRoundRect(btnX, btnY, btnW, btnH, 10, 10);
        g2d.setColor(isBackHovered ? Color.WHITE : new Color(129, 161, 193));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawRoundRect(btnX, btnY, btnW, btnH, 10, 10);

        String backStr = dataManager.getLocalizedString(currentLang, "ui", "back_to_menu");
        g2d.setFont(new Font("SansSerif", Font.BOLD, 15));
        g2d.setColor(isBackHovered ? new Color(30, 32, 48) : Color.WHITE);
        FontMetrics bfm = g2d.getFontMetrics();
        g2d.drawString(backStr, btnX + (btnW - bfm.stringWidth(backStr)) / 2, btnY + (btnH + bfm.getAscent() - bfm.getDescent()) / 2);
    }
}
