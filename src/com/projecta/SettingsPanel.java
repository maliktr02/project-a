package com.projecta;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SettingsPanel {

    public interface SettingsListener {
        void onBack();
        void onToggleFullscreen();
        void onToggleLanguage();
        void onVolumeChanged(int sfxVol, int musicVol);
    }

    private final DataManager dataManager;
    private final AudioEngine audioEngine;
    private final SettingsListener listener;

    private String currentLang = "tr";
    private boolean isFullscreen = false;
    private int sfxVolume = 80;
    private int musicVolume = 70;

    private final List<Rectangle> buttonBounds = new ArrayList<>();
    private int hoveredButton = -1;

    public SettingsPanel(DataManager dataManager, AudioEngine audioEngine, String currentLang, boolean isFullscreen, SettingsListener listener) {
        this.dataManager = dataManager;
        this.audioEngine = audioEngine;
        this.currentLang = currentLang;
        this.isFullscreen = isFullscreen;
        this.listener = listener;
    }

    public void setFullscreenState(boolean fullscreen) {
        this.isFullscreen = fullscreen;
    }

    public void mouseMoved(Point p) {
        int prev = hoveredButton;
        hoveredButton = -1;
        for (int i = 0; i < buttonBounds.size(); i++) {
            if (buttonBounds.get(i).contains(p)) {
                hoveredButton = i;
                break;
            }
        }
        if (hoveredButton != prev && hoveredButton != -1) {
            audioEngine.playButtonClickSound();
        }
    }

    public void mouseClicked(Point p) {
        for (int i = 0; i < buttonBounds.size(); i++) {
            if (buttonBounds.get(i).contains(p)) {
                audioEngine.playButtonClickSound();
                switch (i) {
                    case 0: // Display Mode Toggle
                        isFullscreen = !isFullscreen;
                        listener.onToggleFullscreen();
                        break;
                    case 1: // Language Toggle
                        currentLang = currentLang.equals("tr") ? "en" : "tr";
                        listener.onToggleLanguage();
                        break;
                    case 2: // SFX Volume toggle (- / +)
                        sfxVolume = (sfxVolume >= 100) ? 0 : sfxVolume + 20;
                        listener.onVolumeChanged(sfxVolume, musicVolume);
                        break;
                    case 3: // Music Volume toggle (- / +)
                        musicVolume = (musicVolume >= 100) ? 0 : musicVolume + 20;
                        listener.onVolumeChanged(sfxVolume, musicVolume);
                        break;
                    case 4: // Back
                        listener.onBack();
                        break;
                }
                break;
            }
        }
    }

    public void draw(Graphics2D g2d, int width, int height) {
        // Dark Background
        g2d.setColor(new Color(18, 19, 28));
        g2d.fillRect(0, 0, width, height);

        // Title
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 42));
        FontMetrics tfm = g2d.getFontMetrics();
        String title = dataManager.getLocalizedString(currentLang, "ui", "settings_title");
        g2d.drawString(title, (width - tfm.stringWidth(title)) / 2, 90);

        // Settings Container Card
        int cardW = 500;
        int cardH = 380;
        int cardX = (width - cardW) / 2;
        int cardY = 130;

        g2d.setColor(new Color(46, 52, 64, 200));
        g2d.fillRoundRect(cardX, cardY, cardW, cardH, 20, 20);
        g2d.setColor(new Color(76, 86, 106));
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawRoundRect(cardX, cardY, cardW, cardH, 20, 20);

        buttonBounds.clear();

        // 1. Display Mode Option
        int rowY = cardY + 45;
        drawSettingRow(g2d, cardX + 30, rowY, dataManager.getLocalizedString(currentLang, "ui", "display_mode"), 
            isFullscreen ? dataManager.getLocalizedString(currentLang, "ui", "fullscreen") : dataManager.getLocalizedString(currentLang, "ui", "windowed"), 0);

        // 2. Language Option
        rowY += 60;
        drawSettingRow(g2d, cardX + 30, rowY, dataManager.getLocalizedString(currentLang, "main_menu", "main_menu_language"),
            currentLang.equals("tr") ? "Türkçe (TR)" : "English (EN)", 1);

        // 3. SFX Volume Option
        rowY += 60;
        drawSettingRow(g2d, cardX + 30, rowY, dataManager.getLocalizedString(currentLang, "ui", "sfx_volume"),
            sfxVolume + "%", 2);

        // 4. Music Volume Option
        rowY += 60;
        drawSettingRow(g2d, cardX + 30, rowY, dataManager.getLocalizedString(currentLang, "ui", "music_volume"),
            musicVolume + "%", 3);

        // 5. Back Button
        int btnW = 200;
        int btnH = 44;
        int btnX = (width - btnW) / 2;
        int btnY = cardY + cardH - 65;
        Rectangle backRect = new Rectangle(btnX, btnY, btnW, btnH);
        buttonBounds.add(backRect);

        boolean isHovered = (hoveredButton == 4);
        g2d.setColor(isHovered ? new Color(136, 192, 208) : new Color(59, 66, 82));
        g2d.fillRoundRect(btnX, btnY, btnW, btnH, 12, 12);
        g2d.setColor(isHovered ? Color.WHITE : new Color(129, 161, 193));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawRoundRect(btnX, btnY, btnW, btnH, 12, 12);

        String backStr = dataManager.getLocalizedString(currentLang, "ui", "back_to_menu");
        g2d.setFont(new Font("SansSerif", Font.BOLD, 16));
        g2d.setColor(isHovered ? new Color(30, 32, 48) : Color.WHITE);
        FontMetrics bfm = g2d.getFontMetrics();
        g2d.drawString(backStr, btnX + (btnW - bfm.stringWidth(backStr)) / 2, btnY + (btnH + bfm.getAscent() - bfm.getDescent()) / 2);
    }

    private void drawSettingRow(Graphics2D g2d, int x, int y, String label, String valueStr, int buttonIndex) {
        g2d.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2d.setColor(new Color(236, 239, 244));
        g2d.drawString(label, x, y + 25);

        int valBtnW = 160;
        int valBtnH = 38;
        int valBtnX = x + 280;
        int valBtnY = y;

        Rectangle rect = new Rectangle(valBtnX, valBtnY, valBtnW, valBtnH);
        buttonBounds.add(rect);

        boolean isHovered = (hoveredButton == buttonIndex);
        g2d.setColor(isHovered ? new Color(136, 192, 208) : new Color(67, 76, 94));
        g2d.fillRoundRect(valBtnX, valBtnY, valBtnW, valBtnH, 10, 10);
        g2d.setColor(new Color(129, 161, 193));
        g2d.setStroke(new BasicStroke(1.2f));
        g2d.drawRoundRect(valBtnX, valBtnY, valBtnW, valBtnH, 10, 10);

        g2d.setFont(new Font("SansSerif", Font.BOLD, 15));
        g2d.setColor(isHovered ? new Color(30, 32, 48) : Color.WHITE);
        FontMetrics vfm = g2d.getFontMetrics();
        g2d.drawString(valueStr, valBtnX + (valBtnW - vfm.stringWidth(valueStr)) / 2, valBtnY + (valBtnH + vfm.getAscent() - vfm.getDescent()) / 2);
    }
}
