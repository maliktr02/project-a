package com.projecta;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferStrategy;
import java.io.File;
import javax.swing.ImageIcon;
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
    private final SaveManager saveManager;
    private final AchievementManager achievementManager;

    private GameState currentState = GameState.INTRO;
    private String currentLang = "tr";
    private boolean isFullscreen = false;

    private MainMenuPanel mainMenuPanel;
    private MainGamePanel mainGamePanel;
    private SettingsPanel settingsPanel;
    private AchievementsPanel achievementsPanel;

    private Canvas canvas;
    private BufferStrategy bufferStrategy;
    private boolean isRunning = false;
    private Thread gameThread;

    public GameWindow(File rootDir) {
        // Load All Configs & Binaries
        dataManager = new DataManager();
        dataManager.loadData(rootDir);

        saveManager = new SaveManager(rootDir);
        audioEngine = new AudioEngine();
        audioEngine.init(rootDir);
        achievementManager = new AchievementManager(dataManager);

        setTitle(dataManager.getGameIdentity().getOrDefault("title", "Project A").toString());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
        
        File iconFile = new File(rootDir, "gfxgui/2d/icon.png");
        if (iconFile.exists()) {
            setIconImage(new ImageIcon(iconFile.getAbsolutePath()).getImage());
        }

        currentLang = saveManager.getSetting("language", "tr");
        isFullscreen = saveManager.getSettingBool("fullscreen", false);

        initUI();
    }

    private void initUI() {
        canvas = new Canvas();
        canvas.setFocusable(true);
        canvas.setBackground(new Color(18, 19, 28));

        add(canvas);
        setWindowedMode(900, 900);

        if (isFullscreen) {
            toggleFullscreen();
            isFullscreen = true; // ensure state is correct
        }

        // Sub-Panels
        mainMenuPanel = new MainMenuPanel(dataManager, audioEngine, currentLang, new MainMenuPanel.MenuListener() {
            @Override
            public void onPlay() {
                if (mainGamePanel == null) {
                    mainGamePanel = new MainGamePanel(dataManager, audioEngine, saveManager, achievementManager, currentLang, () -> currentState = GameState.MAIN_MENU);
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

        settingsPanel = new SettingsPanel(dataManager, audioEngine, saveManager, currentLang, isFullscreen, new SettingsPanel.SettingsListener() {
            @Override
            public void onBack() { currentState = GameState.MAIN_MENU; }

            @Override
            public void onToggleFullscreen() { toggleFullscreen(); }

            @Override
            public void onToggleLanguage() {
                currentLang = currentLang.equals("tr") ? "en" : "tr";
                mainMenuPanel.setLanguage(currentLang);
                if (mainGamePanel != null) mainGamePanel.setLanguage(currentLang);
                if (achievementsPanel != null) achievementsPanel.setLanguage(currentLang);
            }

            @Override
            public void onVolumeChanged(int sfxVol, int musicVol) {
                audioEngine.setVolumes(sfxVol, musicVol, sfxVol > 0, musicVol > 0);
            }
        });

        achievementsPanel = new AchievementsPanel(dataManager, saveManager, achievementManager, currentLang, () -> currentState = GameState.MAIN_MENU);

        // Input Listeners
        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Point p = e.getPoint();
                int w = canvas.getWidth();
                int h = canvas.getHeight();

                switch (currentState) {
                    case INTRO: break;
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
                    case INTRO: break;
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
                if (currentState == GameState.MAIN_MENU && mainMenuPanel != null) {
                    mainMenuPanel.keyPressed(e.getKeyCode());
                } else if (currentState == GameState.GAME && mainGamePanel != null) {
                    mainGamePanel.keyPressed(e.getKeyCode());
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
        
        // Start intro audio
        audioEngine.playIntroSound();
        double introTimer = 0.0;
        boolean introFinished = false;

        while (isRunning) {
            long now = System.nanoTime();
            double dt = (now - lastTime) / 1_000_000_000.0;
            lastTime = now;

            // Clamp dt to avoid physics spiral
            if (dt > 0.1) dt = 0.1;
            
            if (currentState == GameState.INTRO && !introFinished) {
                introTimer += dt;
                // Wait approx 3 seconds for intro or until audio finishes
                if (introTimer > 3.0) {
                    introFinished = true;
                    currentState = GameState.MAIN_MENU;
                    audioEngine.startBackgroundMusic();
                }
            }

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
            case INTRO: break;
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
                try {
                    File introFile = new File("gfxgui/intro.png");
                    if (introFile.exists()) {
                        Image introImg = new ImageIcon(introFile.getAbsolutePath()).getImage();
                        g2d.drawImage(introImg, (w - introImg.getWidth(null))/2, (h - introImg.getHeight(null))/2, null);
                    } else {
                        g2d.setColor(Color.WHITE);
                        g2d.setFont(new Font("SansSerif", Font.BOLD, 48));
                        String loading = "PROJECT A";
                        FontMetrics fm = g2d.getFontMetrics();
                        g2d.drawString(loading, (w - fm.stringWidth(loading))/2, h/2);
                    }
                } catch (Exception e) {}
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
