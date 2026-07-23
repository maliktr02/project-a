package com.projecta;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainMenuPanel {

    public interface MenuListener {
        void onPlay();
        void onSettings();
        void onAchievements();
        void onQuit();
    }

    private static class MenuParticle {
        double x, y, vx, vy, size;
        Color color;

        MenuParticle(double x, double y, double vx, double vy, double size, Color color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.color = color;
        }

        void update(double dt, int w, int h) {
            x += vx * dt;
            y += vy * dt;
            if (x < 0) x = w;
            if (x > w) x = 0;
            if (y < 0) y = h;
            if (y > h) y = 0;
        }
    }

    private final DataManager dataManager;
    private final SaveManager saveManager;
    private final AudioEngine audioEngine;
    private final MenuListener listener;
    private String currentLang = "tr";

    private final List<MenuParticle> bgParticles = new ArrayList<>();
    private final Random rand = new Random();

    // Buttons
    private final List<Rectangle> buttonBounds = new ArrayList<>();
    private int hoveredButton = -1;
    private final String[] buttonKeys = new String[]{"main_menu_play", "main_menu_settings", "main_menu_achievements", "main_menu_quit"};
    
    // Credits
    private double creditsTimer = 0;
    private int currentCreditIndex = 1;

    // Konami Code
    private final int[] konamiCode = {KeyEvent.VK_UP, KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_B, KeyEvent.VK_A};
    private int konamiIndex = 0;
    private boolean konamiUnlocked = false;

    public MainMenuPanel(DataManager dataManager, SaveManager saveManager, AudioEngine audioEngine, String currentLang, MenuListener listener) {
        this.dataManager = dataManager;
        this.saveManager = saveManager;
        this.audioEngine = audioEngine;
        this.currentLang = currentLang;
        this.listener = listener;

        // Initialize 30 ambient background particles
        Color[] palette = new Color[]{
            new Color(136, 192, 208, 60),
            new Color(242, 177, 121, 60),
            new Color(246, 124, 95, 60),
            new Color(237, 207, 114, 60)
        };

        for (int i = 0; i < 30; i++) {
            bgParticles.add(new MenuParticle(
                rand.nextInt(1280), rand.nextInt(720),
                (rand.nextDouble() - 0.5) * 30, (rand.nextDouble() - 0.5) * 30,
                8 + rand.nextDouble() * 30,
                palette[rand.nextInt(palette.length)]
            ));
        }
    }

    public void setLanguage(String lang) {
        this.currentLang = lang;
    }

    public void update(double dt, int width, int height) {
        for (MenuParticle p : bgParticles) {
            p.update(dt, width, height);
        }
        
        creditsTimer += dt;
        if (creditsTimer > 4.0) {
            creditsTimer = 0;
            currentCreditIndex++;
            if (currentCreditIndex > 5) currentCreditIndex = 1; // loop 1-5
        }
    }

    public void keyPressed(int keyCode) {
        if (konamiCode[konamiIndex] == keyCode) {
            konamiIndex++;
            if (konamiIndex == konamiCode.length) {
                konamiUnlocked = true;
                konamiIndex = 0;
                audioEngine.playTauntSound();
            }
        } else {
            konamiIndex = 0;
        }
    }

    public void mouseMoved(Point p, int width, int height) {
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
                    case 0: listener.onPlay(); break;
                    case 1: listener.onSettings(); break;
                    case 2: listener.onAchievements(); break;
                    case 3: listener.onQuit(); break;
                }
                break;
            }
        }
    }

    public void draw(Graphics2D g2d, int width, int height) {
        // Gradient Background
        GradientPaint bgGradient = new GradientPaint(
            0, 0, new Color(18, 19, 28),
            width, height, new Color(30, 32, 48)
        );
        g2d.setPaint(bgGradient);
        g2d.fillRect(0, 0, width, height);

        // Draw ambient background particles
        for (MenuParticle p : bgParticles) {
            g2d.setColor(p.color);
            g2d.fillOval((int) p.x, (int) p.y, (int) p.size, (int) p.size);
        }

        // Main Title Header
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 68));
        FontMetrics titleFm = g2d.getFontMetrics();
        String titleStr = "PROJECT A";
        int titleX = (width - titleFm.stringWidth(titleStr)) / 2;
        int titleY = height / 3 - 40;

        // Glowing title text
        g2d.setColor(new Color(136, 192, 208, 100));
        g2d.drawString(titleStr, titleX - 2, titleY - 2);
        g2d.drawString(titleStr, titleX + 2, titleY + 2);
        g2d.setColor(new Color(236, 239, 244));
        g2d.drawString(titleStr, titleX, titleY);

        // Glassmorphic Menu Card
        int cardW = 340;
        int cardH = 260;
        int cardX = (width - cardW) / 2;
        int cardY = titleY + 50;

        g2d.setColor(new Color(36, 42, 54, 180));
        g2d.fillRoundRect(cardX, cardY, cardW, cardH, 20, 20);
        g2d.setColor(new Color(76, 86, 106, 100));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawRoundRect(cardX, cardY, cardW, cardH, 20, 20);

        // Buttons rendering
        buttonBounds.clear();
        int btnW = 280;
        int btnH = 46;
        int startY = cardY + 25;

        for (int i = 0; i < buttonKeys.length; i++) {
            int btnX = (width - btnW) / 2;
            int btnY = startY + i * 55;
            Rectangle rect = new Rectangle(btnX, btnY, btnW, btnH);
            buttonBounds.add(rect);

            boolean isHovered = (hoveredButton == i);

            // Button body
            if (isHovered) {
                g2d.setColor(new Color(136, 192, 208));
            } else {
                g2d.setColor(new Color(59, 66, 82, 180));
            }
            g2d.fillRoundRect(btnX, btnY, btnW, btnH, 12, 12);

            // Button border
            g2d.setColor(isHovered ? Color.WHITE : new Color(129, 161, 193, 150));
            g2d.setStroke(new BasicStroke(1.2f));
            g2d.drawRoundRect(btnX, btnY, btnW, btnH, 12, 12);

            // Button label
            String label = dataManager.getLocalizedString(currentLang, "main_menu", buttonKeys[i]);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 18));
            g2d.setColor(isHovered ? new Color(30, 32, 48) : Color.WHITE);
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(label, btnX + (btnW - fm.stringWidth(label)) / 2, btnY + (btnH + fm.getAscent() - fm.getDescent()) / 2);
        }

        // Footer Credits
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g2d.setColor(new Color(143, 188, 187, 180));
        String credits = dataManager.getLocalizedString(currentLang, "credits", "credits_" + currentCreditIndex);
        if (credits == null || credits.equals("credits_" + currentCreditIndex)) {
            credits = "Project A - Amyun";
        }
        FontMetrics credFm = g2d.getFontMetrics();
        g2d.drawString(credits, (width - credFm.stringWidth(credits)) / 2, height - 30);
        
        if (konamiUnlocked) {
            String secretMsg = dataManager.getLocalizedString(currentLang, "secret", "konami_unlocked");
            if (secretMsg.equals("konami_unlocked")) secretMsg = "DEVELOPER MODE UNLOCKED: YOU ARE A NERD";
            g2d.setColor(new Color(235, 203, 139));
            g2d.drawString(secretMsg, (width - credFm.stringWidth(secretMsg)) / 2, height - 60);
        }
    }
}
