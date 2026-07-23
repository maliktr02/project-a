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
            if (x < -50) x = w + 50;
            if (x > w + 50) x = -50;
            if (y < -50) y = h + 50;
            if (y > h + 50) y = -50;
        }
    }

    private final DataManager dataManager;
    private final AudioEngine audioEngine;
    private final MenuListener listener;
    private String currentLang = "tr";

    private final List<MenuParticle> bgParticles = new ArrayList<>();
    private final Random rand = new Random();

    // Scrolling text
    private double scrollX = 0;
    private final String[] funnyCredits = {
        "Yapımcı: Eren",
        "Tasarımcı: Eren",
        "Bug Üretici: Eren",
        "Çay Getiren: Eren",
        "Oyunu asıl oynayan kişi: Sen (Umarım)",
        "Neden 5 saniye kuralı var diye sorma",
        "Cringe mesajları okumadan geçmeyin"
    };

    // Easter Egg
    private final List<Integer> keyBuffer = new ArrayList<>();
    private final int[] secretCombo = {KeyEvent.VK_UP, KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_DOWN};
    private boolean easterEggFound = false;
    private double easterEggTimer = 0;

    // Buttons
    private final List<Rectangle> buttonBounds = new ArrayList<>();
    private int hoveredButton = -1;
    private final String[] buttonKeys = new String[]{"main_menu_play", "main_menu_settings", "main_menu_achievements", "main_menu_quit"};

    public MainMenuPanel(DataManager dataManager, AudioEngine audioEngine, String currentLang, MenuListener listener) {
        this.dataManager = dataManager;
        this.audioEngine = audioEngine;
        this.currentLang = currentLang;
        this.listener = listener;

        // Softened elegant particles
        Color[] palette = new Color[]{
            new Color(136, 192, 208, 40),
            new Color(242, 177, 121, 40),
            new Color(236, 239, 244, 30),
            new Color(237, 207, 114, 40)
        };

        for (int i = 0; i < 40; i++) {
            bgParticles.add(new MenuParticle(
                rand.nextInt(1280), rand.nextInt(720),
                (rand.nextDouble() - 0.5) * 15, (rand.nextDouble() - 0.5) * 15,
                15 + rand.nextDouble() * 40,
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
        scrollX -= 50 * dt;
        if (scrollX < -1500) {
            scrollX = width + 100;
        }

        if (easterEggFound && easterEggTimer > 0) {
            easterEggTimer -= dt;
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

    public void keyPressed(int keyCode) {
        keyBuffer.add(keyCode);
        if (keyBuffer.size() > 4) {
            keyBuffer.remove(0);
        }
        
        boolean match = true;
        if (keyBuffer.size() == 4) {
            for (int i = 0; i < 4; i++) {
                if (keyBuffer.get(i) != secretCombo[i]) {
                    match = false;
                    break;
                }
            }
            if (match && !easterEggFound) {
                easterEggFound = true;
                easterEggTimer = 5.0; // Show for 5 seconds
                audioEngine.playTauntSound();
            }
        }
    }

    public void draw(Graphics2D g2d, int width, int height) {
        // Smooth Dark Gradient Background
        GradientPaint bgGradient = new GradientPaint(
            0, 0, new Color(15, 16, 25),
            width, height, new Color(25, 27, 40)
        );
        g2d.setPaint(bgGradient);
        g2d.fillRect(0, 0, width, height);

        // Draw ambient background particles
        for (MenuParticle p : bgParticles) {
            g2d.setColor(p.color);
            g2d.fillOval((int) p.x, (int) p.y, (int) p.size, (int) p.size);
        }

        // Main Title Header from Identity
        String titleStr = dataManager.getGameIdentity().getOrDefault("title", "PROJECT A").toString().toUpperCase();
        g2d.setFont(new Font("SansSerif", Font.BOLD, 54));
        FontMetrics titleFm = g2d.getFontMetrics();
        int titleX = (width - titleFm.stringWidth(titleStr)) / 2;
        int titleY = height / 4 - 20;

        // Glowing title text
        g2d.setColor(new Color(136, 192, 208, 100));
        g2d.drawString(titleStr, titleX - 2, titleY - 2);
        g2d.drawString(titleStr, titleX + 2, titleY + 2);
        g2d.setColor(new Color(250, 250, 255));
        g2d.drawString(titleStr, titleX, titleY);

        // Subtitle (Version)
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g2d.setColor(new Color(143, 188, 187));
        String versionStr = "v" + dataManager.getGameIdentity().getOrDefault("version", "1.0.0").toString();
        FontMetrics subFm = g2d.getFontMetrics();
        g2d.drawString(versionStr, (width - subFm.stringWidth(versionStr)) / 2, titleY + 35);

        // Easter Egg Message
        if (easterEggFound && easterEggTimer > 0) {
            g2d.setFont(new Font("SansSerif", Font.BOLD, 22));
            g2d.setColor(new Color(255, 215, 0));
            String eggText = "🎉 GİZLİ KOMBİNASYONU BULDUN! 🎉";
            FontMetrics eggFm = g2d.getFontMetrics();
            g2d.drawString(eggText, (width - eggFm.stringWidth(eggText)) / 2, titleY + 80);
        }

        // Buttons rendering (Minimalist list)
        buttonBounds.clear();
        int btnW = 260;
        int btnH = 45;
        int startY = titleY + 120;

        for (int i = 0; i < buttonKeys.length; i++) {
            int btnX = (width - btnW) / 2;
            int btnY = startY + i * 65; // wider spacing
            Rectangle rect = new Rectangle(btnX, btnY, btnW, btnH);
            buttonBounds.add(rect);

            boolean isHovered = (hoveredButton == i);

            if (isHovered) {
                g2d.setColor(new Color(136, 192, 208, 180));
                g2d.fillRoundRect(btnX - 10, btnY, btnW + 20, btnH, 20, 20);
                
                g2d.setColor(Color.WHITE);
            } else {
                g2d.setColor(new Color(216, 222, 233, 200));
            }

            // Button label (No border, just text for minimalist look)
            String label = dataManager.getLocalizedString(currentLang, "main_menu", buttonKeys[i]);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 22));
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(label.toUpperCase(), btnX + (btnW - fm.stringWidth(label.toUpperCase())) / 2, btnY + (btnH + fm.getAscent() - fm.getDescent()) / 2);
        }

        // Scrolling Footer Credits
        g2d.setFont(new Font("SansSerif", Font.ITALIC, 16));
        g2d.setColor(new Color(143, 188, 187, 180));
        String fullCredits = String.join("   ***   ", funnyCredits);
        
        // initialize scroll position
        if (scrollX == 0) {
            scrollX = width;
        }
        g2d.drawString(fullCredits, (int) scrollX, height - 25);
    }
}
