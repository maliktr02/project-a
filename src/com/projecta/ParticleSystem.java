package com.projecta;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class ParticleSystem {

    private static class Particle {
        double x, y, vx, vy, size;
        Color color;
        double life, maxLife;

        Particle(double x, double y, double vx, double vy, double size, Color color, double maxLife) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.size = size; this.color = color; this.maxLife = maxLife; this.life = 1.0;
        }

        void update(double dt) {
            x += vx * dt; y += vy * dt;
            vy += 180.0 * dt;
            vx *= 0.98;
            life -= dt / maxLife;
        }

        void draw(Graphics2D g2d) {
            if (life <= 0) return;
            float alpha = Math.max(0f, Math.min(1f, (float) life));
            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (alpha * 255)));
            double cs = size * (0.5 + 0.5 * life);
            g2d.fillOval((int) (x - cs / 2), (int) (y - cs / 2), (int) cs, (int) cs);
        }
    }

    private static class FloatingText {
        double x, y;
        String text;
        Color color;
        double life, maxLife;
        int fontSize;

        FloatingText(double x, double y, String text, Color color, int fontSize, double maxLife) {
            this.x = x; this.y = y; this.text = text; this.color = color;
            this.fontSize = fontSize; this.maxLife = maxLife; this.life = 1.0;
        }

        void update(double dt) {
            y -= 30.0 * dt;
            life -= dt / maxLife;
        }

        void draw(Graphics2D g2d) {
            if (life <= 0) return;
            float alpha = Math.max(0f, Math.min(1f, (float) life));
            float scale = 0.8f + 0.4f * (1f - alpha);
            int scaledSize = (int) (fontSize * scale);
            g2d.setFont(new Font("SansSerif", Font.BOLD, scaledSize));
            FontMetrics fm = g2d.getFontMetrics();
            int sw = fm.stringWidth(text);

            g2d.setColor(new Color(0, 0, 0, (int) (alpha * 140)));
            g2d.drawString(text, (int) (x - sw / 2.0 + 1), (int) (y + 1));

            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (alpha * 255)));
            g2d.drawString(text, (int) (x - sw / 2.0), (int) y);
        }
    }

    private final List<Particle> particles = new ArrayList<>();
    private final List<FloatingText> floatingTexts = new ArrayList<>();
    private final Random rand = new Random();

    private double shakeX = 0, shakeY = 0;
    private double shakeIntensity = 0;
    private double shakeDuration = 0;
    private double shakeMaxDuration = 0;
    private double shakeDecay = 0.92;

    public void spawnMergeBurst(double x, double y, Color color, int count) {
        for (int i = 0; i < count; i++) {
            double angle = rand.nextDouble() * Math.PI * 2.0;
            double speed = 60.0 + rand.nextDouble() * 240.0;
            double sz = 3.0 + rand.nextDouble() * 9.0;
            double ml = 0.35 + rand.nextDouble() * 0.55;

            int r = Math.min(255, color.getRed() + rand.nextInt(40) - 20);
            int g = Math.min(255, color.getGreen() + rand.nextInt(40) - 20);
            int b = Math.min(255, color.getBlue() + rand.nextInt(40) - 20);
            Color varied = new Color(Math.max(0, r), Math.max(0, g), Math.max(0, b));

            particles.add(new Particle(x, y, Math.cos(angle) * speed, Math.sin(angle) * speed, sz, varied, ml));
        }
    }

    public void spawnFloatingText(double x, double y, String text, Color color, int fontSize) {
        floatingTexts.add(new FloatingText(x, y, text, color, fontSize, 1.4));
    }

    public void triggerScreenShake(double intensity, double duration) {
        this.shakeIntensity = Math.max(this.shakeIntensity, intensity);
        this.shakeDuration = Math.max(this.shakeDuration, duration);
        this.shakeMaxDuration = Math.max(this.shakeMaxDuration, duration);
    }

    public void update(double dt) {
        Iterator<Particle> pi = particles.iterator();
        while (pi.hasNext()) { Particle p = pi.next(); p.update(dt); if (p.life <= 0) pi.remove(); }

        Iterator<FloatingText> fi = floatingTexts.iterator();
        while (fi.hasNext()) { FloatingText ft = fi.next(); ft.update(dt); if (ft.life <= 0) fi.remove(); }

        if (shakeDuration > 0) {
            shakeDuration -= dt;
            double progress = Math.max(0, shakeMaxDuration > 0 ? shakeDuration / shakeMaxDuration : 0);
            double damp = progress * progress; // ease out quadratically
            double t = (shakeMaxDuration - shakeDuration) * 40.0;
            shakeX = Math.sin(t * 7.3) * shakeIntensity * damp;
            shakeY = Math.cos(t * 5.1) * shakeIntensity * 0.7 * damp;
            if (shakeDuration <= 0) { shakeX = 0; shakeY = 0; shakeIntensity = 0; shakeMaxDuration = 0; }
        }
    }

    public void draw(Graphics2D g2d) {
        for (Particle p : particles) p.draw(g2d);
        for (FloatingText ft : floatingTexts) ft.draw(g2d);
    }

    public double getShakeX() { return shakeX; }
    public double getShakeY() { return shakeY; }

    public void clear() {
        particles.clear();
        floatingTexts.clear();
        shakeDuration = 0; shakeX = 0; shakeY = 0;
    }
}
