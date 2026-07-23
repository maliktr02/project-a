package com.projecta;

import java.awt.*;
import java.awt.geom.Ellipse2D;

public class GameObject {

    public double x;
    public double y;
    public double vx;
    public double vy;
    public double angle = 0.0;
    public double angularVelocity = 0.0;

    public final ObjectConfig config;
    public double radius;
    public double mass;

    public boolean isDropping = false;
    public boolean isSettled = false;
    public double scaleAnim = 0.0; // 0.0 to 1.0 pop spawn animation
    public double squishX = 1.0;
    public double squishY = 1.0;

    public GameObject(double x, double y, ObjectConfig config) {
        this.x = x;
        this.y = y;
        this.config = config;
        this.radius = config.radius;
        this.mass = config.mass;
        this.vx = 0;
        this.vy = 0;
        this.scaleAnim = 0.2;
    }

    public void update(double dt) {
        // Animate scale up on spawn
        if (scaleAnim < 1.0) {
            scaleAnim += dt * 5.0;
            if (scaleAnim > 1.0) scaleAnim = 1.0;
        }

        // Recover squish
        squishX += (1.0 - squishX) * dt * 10.0;
        squishY += (1.0 - squishY) * dt * 10.0;

        // Roll rotation
        angle += angularVelocity * dt;
        angularVelocity *= 0.98;
    }

    public void draw(Graphics2D g2d) {
        double currentRadius = radius * scaleAnim;
        double renderWidth = currentRadius * 2 * squishX;
        double renderHeight = currentRadius * 2 * squishY;
        double renderX = x - renderWidth / 2.0;
        double renderY = y - renderHeight / 2.0;

        // Drop subtle shadow
        g2d.setColor(new Color(0, 0, 0, 45));
        g2d.fill(new Ellipse2D.Double(renderX + 3, renderY + 4, renderWidth, renderHeight));

        // Radial gradient ball background
        Color baseColor = config.color;
        Color lightColor = highlightColor(baseColor);
        Color darkColor = shadowColor(baseColor);

        RadialGradientPaint rgp = new RadialGradientPaint(
            (float) (x - currentRadius * 0.3),
            (float) (y - currentRadius * 0.3),
            (float) (currentRadius * 1.4),
            new float[]{0.0f, 0.7f, 1.0f},
            new Color[]{lightColor, baseColor, darkColor}
        );

        g2d.setPaint(rgp);
        g2d.fill(new Ellipse2D.Double(renderX, renderY, renderWidth, renderHeight));

        // Specular highlight arc
        g2d.setColor(new Color(255, 255, 255, 120));
        g2d.setStroke(new BasicStroke((float) Math.max(1.5, currentRadius * 0.08)));
        g2d.drawArc(
            (int) (renderX + currentRadius * 0.2),
            (int) (renderY + currentRadius * 0.15),
            (int) (currentRadius * 1.2),
            (int) (currentRadius * 1.2),
            45, 90
        );

        // Outer border stroke
        g2d.setColor(darkColor.darker());
        g2d.setStroke(new BasicStroke(1.8f));
        g2d.draw(new Ellipse2D.Double(renderX, renderY, renderWidth, renderHeight));

        // Draw Value Text in Center
        String label = String.valueOf(config.value);
        int fontSize = (int) Math.max(12, currentRadius * 0.75);
        Font font = new Font("SansSerif", Font.BOLD, fontSize);
        g2d.setFont(font);

        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(label);
        int textHeight = fm.getAscent() - fm.getDescent();

        // Text shadow for high contrast
        g2d.setColor(new Color(0, 0, 0, 160));
        g2d.drawString(label, (int) (x - textWidth / 2.0 + 1), (int) (y + textHeight / 2.0 + 1));

        // Text main color (dark text for light background, white for dark)
        g2d.setColor(isColorDark(baseColor) ? Color.WHITE : new Color(40, 40, 40));
        g2d.drawString(label, (int) (x - textWidth / 2.0), (int) (y + textHeight / 2.0));
    }

    private Color highlightColor(Color c) {
        int r = Math.min(255, c.getRed() + 45);
        int g = Math.min(255, c.getGreen() + 45);
        int b = Math.min(255, c.getBlue() + 45);
        return new Color(r, g, b);
    }

    private Color shadowColor(Color c) {
        int r = Math.max(0, c.getRed() - 55);
        int g = Math.max(0, c.getGreen() - 55);
        int b = Math.max(0, c.getBlue() - 55);
        return new Color(r, g, b);
    }

    private boolean isColorDark(Color c) {
        double luminance = (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue()) / 255.0;
        return luminance < 0.6;
    }
}
