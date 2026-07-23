package com.projecta;

import java.io.File;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

    public static void main(String[] args) {
        // Set system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        File currentDir = new File(System.getProperty("user.dir"));
        File rootDir = currentDir;
        if (!new File(currentDir, "version.toml").exists()) {
            if (currentDir.getParentFile() != null && new File(currentDir.getParentFile(), "version.toml").exists()) {
                rootDir = currentDir.getParentFile();
            }
        }

        File finalRootDir = rootDir;
        SwingUtilities.invokeLater(() -> {
            GameWindow window = new GameWindow(finalRootDir);
            window.setVisible(true);
            window.start();
        });
    }
}
