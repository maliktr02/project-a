package com.projecta;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferStrategy;
import java.io.File;
import javax.swing.JFrame;

public class GameWindow extends JFrame implements Runnable {

    public enum GameState {
        INTRO,
        MAIN_MENU,
        GAME,
        SETTINGS,
        ACHIEVEMENTS
    }

    private final DataManager dataManager;
    private final AudioEngine audioEngine;
    private final AchievementManager achievementManager;

    private GameState currentState = GameState.MAIN_MENU;
    private String currentLang = "tr";
    private boolean isFullscreen = false;

    private MainMenuPanel mainMenuPanel;
    private MainGamePanel mainGamePanel;
    private SettingsPanel settingsPanel;
    private AchievementsPanel achievementsPanel;

    private final SaveManager saveManager;

    private Canvas canvas;
    private BufferStrategy bufferStrategy;
    private boolean isRunning = false;
    private Thread gameThread;

    private Image introImage;
    private double introTimer = 0;

    public GameWindow(File rootDir) {
        // Load All Configs & Binaries
        dataManager = new DataManager();
        dataManager.loadData(rootDir);

        saveManager = new SaveManager(rootDir);

        audioEngine = new AudioEngine();
        audioEngine.init(rootDir);
        audioEngine.setVolumes(saveManager.getSettingInt("sfx_volume", 80), saveManager.getSettingInt("music_volume", 70), saveManager.getSettingBool("sfx_enabled", true), saveManager.getSettingBool("music_enabled", true));

        achievementManager = new AchievementManager(dataManager, saveManager);

        try {
            File introFile = new File(rootDir, "gfxgui/intro.png");
            if (introFile.exists()) {
                introImage = javax.imageio.ImageIO.read(introFile);
            }
        } catch (Exception e) {
            GameLogger.get().error("GameWindow", "Failed to load intro.png", e);
        }

        currentState = GameState.INTRO;
        audioEngine.playIntroSound();

        setTitle("Project A - 2048 Physics Merge");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);

        initUI();
    }

    private void initUI() {
        canvas = new Canvas();
        canvas.setFocusable(true);
        canvas.setBackground(new Color(18, 19, 28));

        add(canvas);
        setWindowedMode(1280, 720);

        // Sub-Panels
        mainMenuPanel = new MainMenuPanel(dataManager, saveManager, audioEngine, currentLang, new MainMenuPanel.MenuListener() {
            @Override
            public void onPlay() {
                if (mainGamePanel == null) {
                    mainGamePanel = new MainGamePanel(dataManager, saveManager, audioEngine, achievementManager, currentLang, () -> currentState = GameState.MAIN_MENU);
                } else {
                    mainGamePanel.initGame();
                }
                currentState = GameState.GAME;
            }

            @Override
            public void onSettings() { currentState = GameState.SETTINGS; }

            @Override
            public void onAchievements() { currentState = GameState.ACHIEVEMENTS; }

            @Override
            public void onQuit() { System.exit(0); }
        });

        settingsPanel = new SettingsPanel(dataManager, saveManager, audioEngine, currentLang, isFullscreen, new SettingsPanel.SettingsListener() {
            @Override
            public void onBack() { currentState = GameState.MAIN_MENU; }

            @Override
            public void onToggleFullscreen() { toggleFullscreen(); }

            @Override
            public void onToggleLanguage() {
                currentLang = currentLang.equals("tr") ? "en" : "tr";
                saveManager.setSetting("language", currentLang);
                mainMenuPanel.setLanguage(currentLang);
                if (mainGamePanel != null) mainGamePanel.setLanguage(currentLang);
                if (achievementsPanel != null) achievementsPanel.setLanguage(currentLang);
            }

            @Override
            public void onVolumeChanged(int sfxVol, int musicVol) {
                saveManager.setSetting("sfx_volume", sfxVol);
                saveManager.setSetting("music_volume", musicVol);
                audioEngine.setVolumes(sfxVol, musicVol, sfxVol > 0, musicVol > 0);
            }
        });

        achievementsPanel = new AchievementsPanel(dataManager, achievementManager, currentLang, () -> currentState = GameState.MAIN_MENU);

        // Input Listeners
        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Point p = e.getPoint();
                int w = canvas.getWidth();
                int h = canvas.getHeight();

                switch (currentState) {
                    case MAIN_MENU: mainMenuPanel.mouseMoved(p, w, h); break;
                    case GAME: if (mainGamePanel != null) mainGamePanel.mouseMoved(p); break;
                    case SETTINGS: settingsPanel.mouseMoved(p); break;
                    case ACHIEVEMENTS: achievementsPanel.mouseMoved(p); break;
                }
            }
        });

        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point p = e.getPoint();

                switch (currentState) {
                    case MAIN_MENU: mainMenuPanel.mouseClicked(p); break;
                    case GAME: if (mainGamePanel != null) mainGamePanel.mousePressed(p); break;
                    case SETTINGS: settingsPanel.mouseClicked(p); break;
                    case ACHIEVEMENTS: achievementsPanel.mouseClicked(p); break;
                }
            }
        });

        canvas.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F11) {
                    toggleFullscreen();
                    return;
                }
                if (currentState == GameState.GAME && mainGamePanel != null) {
                    mainGamePanel.keyPressed(e.getKeyCode());
                } else if (currentState == GameState.MAIN_MENU) {
                    mainMenuPanel.keyPressed(e.getKeyCode());
                }
            }
        });
    }

    public synchronized void start() {
        if (isRunning) return;
        isRunning = true;

        canvas.createBufferStrategy(3);
        bufferStrategy = canvas.getBufferStrategy();

        gameThread = new Thread(this, "GameLoop");
        gameThread.start();
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        final double nsPerTick = 1_000_000_000.0 / 60.0; // 60 FPS Target

        while (isRunning) {
            long now = System.nanoTime();
            double dt = (now - lastTime) / 1_000_000_000.0;
            lastTime = now;

            // Clamp dt to avoid physics spiral
            if (dt > 0.1) dt = 0.1;

            update(dt);
            render();

            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {}
        }
    }

    private void update(double dt) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();

        switch (currentState) {
            case INTRO:
                introTimer += dt;
                if (introTimer >= 4.0) {
                    currentState = GameState.MAIN_MENU;
                    audioEngine.startBackgroundMusic();
                }
                break;
            case MAIN_MENU: mainMenuPanel.update(dt, w, h); break;
            case GAME: if (mainGamePanel != null) mainGamePanel.update(dt, w, h); break;
            case SETTINGS: break;
            case ACHIEVEMENTS: break;
        }
    }

    private void render() {
        if (bufferStrategy == null) {
            canvas.createBufferStrategy(3);
            bufferStrategy = canvas.getBufferStrategy();
            return;
        }

        Graphics2D g2d = (Graphics2D) bufferStrategy.getDrawGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = canvas.getWidth();
        int h = canvas.getHeight();

        switch (currentState) {
            case INTRO:
                g2d.setColor(Color.BLACK);
                g2d.fillRect(0, 0, w, h);
                if (introImage != null) {
                    int iw = introImage.getWidth(null);
                    int ih = introImage.getHeight(null);
                    g2d.drawImage(introImage, (w - iw) / 2, (h - ih) / 2, null);
                }
                break;
            case MAIN_MENU: mainMenuPanel.draw(g2d, w, h); break;
            case GAME: if (mainGamePanel != null) mainGamePanel.draw(g2d, w, h); break;
            case SETTINGS: settingsPanel.draw(g2d, w, h); break;
            case ACHIEVEMENTS: achievementsPanel.draw(g2d, w, h); break;
        }

        g2d.dispose();
        bufferStrategy.show();
    }

    private void toggleFullscreen() {
        dispose(); // Temporarily remove window decorations
        isFullscreen = !isFullscreen;

        if (isFullscreen) {
            setUndecorated(true);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();
            gd.setFullScreenWindow(this);
        } else {
            setUndecorated(false);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();
            gd.setFullScreenWindow(null);
            setWindowedMode(1280, 720);
        }

        settingsPanel.setFullscreenState(isFullscreen);
        setVisible(true);
        canvas.requestFocus();
    }

    private void setWindowedMode(int w, int h) {
        canvas.setPreferredSize(new Dimension(w, h));
        pack();
        setLocationRelativeTo(null);
    }
}
