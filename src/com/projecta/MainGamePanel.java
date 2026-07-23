package com.projecta;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainGamePanel {

    public interface GameListener {
        void onReturnToMenu();
    }

    private final DataManager dataManager;
    private final AudioEngine audioEngine;
    private final AchievementManager achievementManager;
    private final GameListener listener;
    private String currentLang = "tr";

    // Systems
    private PhysicsEngine physicsEngine;
    private MergeSystem mergeSystem;
    private DangerSystem dangerSystem;
    private ParticleSystem particleSystem;

    // Game Objects
    private final List<GameObject> objects = new ArrayList<>();
    private GameObject currentSpawnObj;
    private ObjectConfig nextSpawnConfig;

    // Game State
    private long score = 0;
    private long highScore = 0;
    private boolean isPaused = false;
    private boolean isGameOver = false;

    private double aimX = 0;
    private double spawnY = 80;
    private final Random rand = new Random();

    // Container Bucket Bounds
    private double bucketX;
    private double bucketY;
    private double bucketWidth = 460;
    private double bucketHeight = 580;
    private double dangerYOffset = 110; // Danger Line height inside bucket

    // Buttons in Overlay
    private Rectangle resumeBtn, restartBtn, menuBtn;
    private int hoveredOverlayBtn = -1;

    public MainGamePanel(DataManager dataManager, AudioEngine audioEngine, AchievementManager achievementManager, String currentLang, GameListener listener) {
        this.dataManager = dataManager;
        this.audioEngine = audioEngine;
        this.achievementManager = achievementManager;
        this.currentLang = currentLang;
        this.listener = listener;

        initGame();
    }

    public void setLanguage(String lang) {
        this.currentLang = lang;
    }

    public void initGame() {
        physicsEngine = new PhysicsEngine(dataManager.getGravity(), dataManager.getBounceDamping());
        mergeSystem = new MergeSystem(dataManager);
        dangerSystem = new DangerSystem(dataManager.getDangerTimeSeconds());
        particleSystem = new ParticleSystem();

        objects.clear();
        score = 0;
        isPaused = false;
        isGameOver = false;

        nextSpawnConfig = getRandomSpawnConfig();
        prepareNextSpawn();
    }

    private ObjectConfig getRandomSpawnConfig() {
        // Levels 1 to 5 for initial spawn
        int level = 1 + rand.nextInt(5);
        return dataManager.getObjectConfigByLevel(level);
    }

    private void prepareNextSpawn() {
        ObjectConfig config = (nextSpawnConfig != null) ? nextSpawnConfig : getRandomSpawnConfig();
        nextSpawnConfig = getRandomSpawnConfig();

        double startX = bucketX + bucketWidth / 2.0;
        currentSpawnObj = new GameObject(startX, spawnY, config);
        currentSpawnObj.isDropping = false;
        aimX = startX;
    }

    public void update(double dt, int screenWidth, int screenHeight) {
        // Calculate dynamic bucket placement
        bucketWidth = 460;
        bucketHeight = Math.min(580, screenHeight - 140);
        bucketX = (screenWidth - bucketWidth) / 2.0;
        bucketY = 100;
        spawnY = bucketY - 40;

        if (isPaused || isGameOver) return;

        double boundsLeft = bucketX;
        double boundsRight = bucketX + bucketWidth;
        double boundsBottom = bucketY + bucketHeight;
        double dangerY = bucketY + dangerYOffset;

        // 1. Update Spawner Aim Position
        if (currentSpawnObj != null && !currentSpawnObj.isDropping) {
            double minX = boundsLeft + currentSpawnObj.radius;
            double maxX = boundsRight - currentSpawnObj.radius;
            currentSpawnObj.x = Math.max(minX, Math.min(maxX, aimX));
            currentSpawnObj.y = spawnY;
            currentSpawnObj.update(dt);
        }

        // 2. Update Physics
        physicsEngine.update(objects, dt, boundsLeft, boundsRight, boundsBottom);

        // 3. Update active objects scale/animations
        for (GameObject obj : objects) {
            obj.update(dt);
        }

        // 4. Update Merges
        mergeSystem.update(dt);
        List<MergeSystem.MergeEvent> mergeEvents = mergeSystem.checkAndPerformMerges(objects);

        for (MergeSystem.MergeEvent event : mergeEvents) {
            score += event.scoreGained;
            if (score > highScore) highScore = score;

            // FX
            particleSystem.spawnMergeBurst(event.x, event.y, event.newConfig.color, 18 + event.newLevel * 2);
            particleSystem.spawnFloatingText(event.x, event.y - 15, "+" + event.scoreGained, event.newConfig.color.brighter(), 22);

            audioEngine.playMergeSound(event.newLevel);

            if (mergeSystem.getComboCount() >= 2) {
                particleSystem.spawnFloatingText(event.x, event.y - 45, "KOMBO x" + mergeSystem.getComboCount(), new Color(255, 215, 0), 24);
                audioEngine.playComboSound(mergeSystem.getComboCount());
            }

            particleSystem.triggerScreenShake(3.0 + event.newLevel, 0.2);

            // Achievements check
            achievementManager.checkMergeAchievements(event.newLevel, mergeSystem.getComboCount(), score, dangerSystem.isDangerActive(), audioEngine);
        }

        // 5. Danger & Game Over Check
        boolean wasDanger = dangerSystem.isDangerActive();
        dangerSystem.update(objects, dangerY, dt);

        if (dangerSystem.isDangerActive() && !wasDanger) {
            audioEngine.playDangerSound();
            particleSystem.spawnFloatingText(bucketX + bucketWidth / 2.0, dangerY - 20, "TEHLİKE!", new Color(255, 51, 102), 26);
        }

        if (dangerSystem.isGameOver() && !isGameOver) {
            isGameOver = true;
            audioEngine.playGameOverSound();
            particleSystem.triggerScreenShake(12.0, 0.6);
        }

        // 6. Particle & Toasts Update
        particleSystem.update(dt);
        achievementManager.update(dt);
    }

    public void mouseMoved(Point p) {
        aimX = p.x;
        updateOverlayHover(p);
    }

    public void mousePressed(Point p) {
        if (isPaused || isGameOver) {
            handleOverlayClick(p);
            return;
        }

        // Drop current object
        dropObject();
    }

    public void keyPressed(int keyCode) {
        if (keyCode == KeyEvent.VK_P || keyCode == KeyEvent.VK_ESCAPE) {
            if (!isGameOver) {
                isPaused = !isPaused;
                audioEngine.playButtonClickSound();
            }
        } else if (keyCode == KeyEvent.VK_SPACE || keyCode == KeyEvent.VK_DOWN) {
            if (!isPaused && !isGameOver) {
                dropObject();
            }
        } else if (keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_A) {
            aimX -= 25;
        } else if (keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_D) {
            aimX += 25;
        }
    }

    private void dropObject() {
        if (currentSpawnObj != null && !currentSpawnObj.isDropping) {
            currentSpawnObj.isDropping = true;
            objects.add(currentSpawnObj);
            audioEngine.playDropSound();

            currentSpawnObj = null;

            // Schedule next object spawn after brief delay
            prepareNextSpawn();
        }
    }

    public void draw(Graphics2D g2d, int screenWidth, int screenHeight) {
        // Screen Shake Transform
        AffineTransform oldTransform = g2d.getTransform();
        g2d.translate(particleSystem.getShakeX(), particleSystem.getShakeY());

        // Background
        g2d.setColor(new Color(18, 19, 28));
        g2d.fillRect(-50, -50, screenWidth + 100, screenHeight + 100);

        // 1. Draw Score Panel Header (Top-Left)
        drawScoreHeader(g2d);

        // 2. Draw Next Object Preview Box (Top-Right)
        drawNextPreview(g2d, screenWidth);

        // 3. Draw Game Container Bucket
        drawContainerBucket(g2d);

        // 4. Draw Aim Line
        if (currentSpawnObj != null && !currentSpawnObj.isDropping && !isPaused && !isGameOver) {
            g2d.setColor(new Color(136, 192, 208, 140));
            g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{8, 8}, 0));
            g2d.draw(new Line2D.Double(currentSpawnObj.x, currentSpawnObj.y, currentSpawnObj.x, bucketY + bucketHeight));

            currentSpawnObj.draw(g2d);
        }

        // 5. Draw Dropped Objects
        for (GameObject obj : objects) {
            obj.draw(g2d);
        }

        // 6. Draw Particles & Toasts
        particleSystem.draw(g2d);
        achievementManager.drawToasts(g2d, screenWidth);

        g2d.setTransform(oldTransform);

        // 7. Draw Pause / Game Over Overlays
        if (isPaused) {
            drawPauseOverlay(g2d, screenWidth, screenHeight);
        } else if (isGameOver) {
            drawGameOverOverlay(g2d, screenWidth, screenHeight);
        }
    }

    private void drawScoreHeader(Graphics2D g2d) {
        int x = 40;
        int y = 30;

        g2d.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2d.setColor(new Color(143, 188, 187));
        g2d.drawString(dataManager.getLocalizedString(currentLang, "ui", "score").toUpperCase(), x, y);

        g2d.setFont(new Font("SansSerif", Font.BOLD, 32));
        g2d.setColor(Color.WHITE);
        g2d.drawString(String.valueOf(score), x, y + 32);

        g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2d.setColor(new Color(235, 203, 139));
        g2d.drawString(dataManager.getLocalizedString(currentLang, "ui", "high_score").toUpperCase() + ": " + highScore, x, y + 54);
    }

    private void drawNextPreview(Graphics2D g2d, int screenWidth) {
        int boxW = 120;
        int boxH = 90;
        int boxX = screenWidth - boxW - 40;
        int boxY = 25;

        g2d.setColor(new Color(46, 52, 64, 200));
        g2d.fillRoundRect(boxX, boxY, boxW, boxH, 14, 14);
        g2d.setColor(new Color(76, 86, 106));
        g2d.setStroke(new BasicStroke(1.8f));
        g2d.drawRoundRect(boxX, boxY, boxW, boxH, 14, 14);

        g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2d.setColor(new Color(143, 188, 187));
        FontMetrics fm = g2d.getFontMetrics();
        String lbl = dataManager.getLocalizedString(currentLang, "ui", "next_object").toUpperCase();
        g2d.drawString(lbl, boxX + (boxW - fm.stringWidth(lbl)) / 2, boxY + 20);

        if (nextSpawnConfig != null) {
            double previewX = boxX + boxW / 2.0;
            double previewY = boxY + 54;
            GameObject dummy = new GameObject(previewX, previewY, nextSpawnConfig);
            dummy.scaleAnim = 0.8;
            dummy.draw(g2d);
        }
    }

    private void drawContainerBucket(Graphics2D g2d) {
        // Container Glass Fill
        g2d.setColor(new Color(26, 28, 40, 210));
        g2d.fill(new RoundRectangle2D.Double(bucketX, bucketY, bucketWidth, bucketHeight, 20, 20));

        // Danger Line
        double dangerY = bucketY + dangerYOffset;
        boolean isDanger = dangerSystem.isDangerActive();

        g2d.setColor(isDanger ? new Color(255, 51, 102, 230) : new Color(255, 51, 102, 110));
        g2d.setStroke(new BasicStroke(isDanger ? 3.0f : 1.8f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{10, 6}, 0));
        g2d.draw(new Line2D.Double(bucketX, dangerY, bucketX + bucketWidth, dangerY));

        // Danger Line Label & Pulsing Bar
        if (isDanger) {
            g2d.setFont(new Font("SansSerif", Font.BOLD, 14));
            g2d.setColor(new Color(255, 51, 102));
            g2d.drawString("⚠️ TEHLİKE! (" + String.format("%.1fs", dangerSystem.getCurrentDangerTime()) + ")", (int) (bucketX + 15), (int) (dangerY - 8));

            // Red warning border glow around container
            g2d.setColor(new Color(255, 51, 102, 160));
            g2d.setStroke(new BasicStroke(5.0f));
            g2d.draw(new RoundRectangle2D.Double(bucketX, bucketY, bucketWidth, bucketHeight, 20, 20));
        }

        // Outer Glass Border Frame
        g2d.setColor(new Color(76, 86, 106));
        g2d.setStroke(new BasicStroke(4.0f));
        g2d.draw(new RoundRectangle2D.Double(bucketX, bucketY, bucketWidth, bucketHeight, 20, 20));
    }

    private void drawPauseOverlay(Graphics2D g2d, int width, int height) {
        g2d.setColor(new Color(15, 17, 26, 210));
        g2d.fillRect(0, 0, width, height);

        int cardW = 340;
        int cardH = 260;
        int cardX = (width - cardW) / 2;
        int cardY = (height - cardH) / 2;

        g2d.setColor(new Color(46, 52, 64, 230));
        g2d.fillRoundRect(cardX, cardY, cardW, cardH, 20, 20);

        g2d.setFont(new Font("SansSerif", Font.BOLD, 32));
        g2d.setColor(Color.WHITE);
        FontMetrics fm = g2d.getFontMetrics();
        String pStr = dataManager.getLocalizedString(currentLang, "ui", "paused");
        g2d.drawString(pStr, (width - fm.stringWidth(pStr)) / 2, cardY + 55);

        setupOverlayButtons(width, cardY + 90);
        drawOverlayButtons(g2d, new String[]{
            dataManager.getLocalizedString(currentLang, "ui", "resume"),
            dataManager.getLocalizedString(currentLang, "ui", "restart"),
            dataManager.getLocalizedString(currentLang, "ui", "back_to_menu")
        });
    }

    private void drawGameOverOverlay(Graphics2D g2d, int width, int height) {
        g2d.setColor(new Color(25, 12, 18, 220));
        g2d.fillRect(0, 0, width, height);

        int cardW = 380;
        int cardH = 300;
        int cardX = (width - cardW) / 2;
        int cardY = (height - cardH) / 2;

        g2d.setColor(new Color(46, 52, 64, 240));
        g2d.fillRoundRect(cardX, cardY, cardW, cardH, 20, 20);
        g2d.setColor(new Color(255, 51, 102));
        g2d.setStroke(new BasicStroke(2.5f));
        g2d.drawRoundRect(cardX, cardY, cardW, cardH, 20, 20);

        g2d.setFont(new Font("SansSerif", Font.BOLD, 36));
        g2d.setColor(new Color(255, 51, 102));
        FontMetrics fm = g2d.getFontMetrics();
        String goStr = dataManager.getLocalizedString(currentLang, "ui", "game_over");
        g2d.drawString(goStr, (width - fm.stringWidth(goStr)) / 2, cardY + 50);

        g2d.setFont(new Font("SansSerif", Font.BOLD, 20));
        g2d.setColor(Color.WHITE);
        String finalScoreStr = dataManager.getLocalizedString(currentLang, "ui", "score") + ": " + score;
        FontMetrics sfm = g2d.getFontMetrics();
        g2d.drawString(finalScoreStr, (width - sfm.stringWidth(finalScoreStr)) / 2, cardY + 85);

        setupOverlayButtons(width, cardY + 120);
        drawOverlayButtons(g2d, new String[]{
            dataManager.getLocalizedString(currentLang, "ui", "restart"),
            dataManager.getLocalizedString(currentLang, "ui", "back_to_menu"),
            ""
        });
    }

    private void setupOverlayButtons(int width, int startY) {
        int btnW = 240;
        int btnH = 44;
        int btnX = (width - btnW) / 2;

        resumeBtn = new Rectangle(btnX, startY, btnW, btnH);
        restartBtn = new Rectangle(btnX, startY + 55, btnW, btnH);
        menuBtn = new Rectangle(btnX, startY + 110, btnW, btnH);
    }

    private void drawOverlayButtons(Graphics2D g2d, String[] labels) {
        Rectangle[] btns = new Rectangle[]{resumeBtn, restartBtn, menuBtn};

        for (int i = 0; i < btns.length; i++) {
            if (labels[i].isEmpty()) continue;
            Rectangle r = btns[i];
            boolean isHovered = (hoveredOverlayBtn == i);

            g2d.setColor(isHovered ? new Color(136, 192, 208) : new Color(59, 66, 82));
            g2d.fillRoundRect(r.x, r.y, r.width, r.height, 12, 12);
            g2d.setColor(isHovered ? Color.WHITE : new Color(129, 161, 193));
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawRoundRect(r.x, r.y, r.width, r.height, 12, 12);

            g2d.setFont(new Font("SansSerif", Font.BOLD, 16));
            g2d.setColor(isHovered ? new Color(30, 32, 48) : Color.WHITE);
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(labels[i], r.x + (r.width - fm.stringWidth(labels[i])) / 2, r.y + (r.height + fm.getAscent() - fm.getDescent()) / 2);
        }
    }

    private void updateOverlayHover(Point p) {
        if (resumeBtn == null) return;
        int prev = hoveredOverlayBtn;
        hoveredOverlayBtn = -1;
        if (isPaused) {
            if (resumeBtn.contains(p)) hoveredOverlayBtn = 0;
            else if (restartBtn.contains(p)) hoveredOverlayBtn = 1;
            else if (menuBtn.contains(p)) hoveredOverlayBtn = 2;
        } else if (isGameOver) {
            if (resumeBtn.contains(p)) hoveredOverlayBtn = 0; // Restart
            else if (restartBtn.contains(p)) hoveredOverlayBtn = 1; // Menu
        }

        if (hoveredOverlayBtn != prev && hoveredOverlayBtn != -1) {
            audioEngine.playButtonClickSound();
        }
    }

    private void handleOverlayClick(Point p) {
        if (isPaused) {
            if (resumeBtn.contains(p)) {
                isPaused = false;
                audioEngine.playButtonClickSound();
            } else if (restartBtn.contains(p)) {
                initGame();
                audioEngine.playButtonClickSound();
            } else if (menuBtn.contains(p)) {
                audioEngine.playButtonClickSound();
                listener.onReturnToMenu();
            }
        } else if (isGameOver) {
            if (resumeBtn.contains(p)) {
                initGame();
                audioEngine.playButtonClickSound();
            } else if (restartBtn.contains(p)) {
                audioEngine.playButtonClickSound();
                listener.onReturnToMenu();
            }
        }
    }
}
