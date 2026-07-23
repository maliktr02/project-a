package com.projecta;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GameLogger {

    public enum Level { DEBUG, INFO, WARN, ERROR }

    private static GameLogger instance;
    private Level minLevel = Level.DEBUG;
    private PrintWriter fileWriter;
    private boolean consoleEnabled = true;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private GameLogger() {}

    public static synchronized GameLogger get() {
        if (instance == null) instance = new GameLogger();
        return instance;
    }

    public void init(File logDir) {
        try {
            if (!logDir.exists()) logDir.mkdirs();
            String fileName = "projecta_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".log";
            File logFile = new File(logDir, fileName);
            fileWriter = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)), true);
            info("GameLogger", "Log initialized: " + logFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to initialize logger: " + e.getMessage());
        }
    }

    public void setLevel(Level level) { this.minLevel = level; }
    public void setConsoleEnabled(boolean enabled) { this.consoleEnabled = enabled; }

    public void debug(String tag, String msg) { log(Level.DEBUG, tag, msg); }
    public void info(String tag, String msg) { log(Level.INFO, tag, msg); }
    public void warn(String tag, String msg) { log(Level.WARN, tag, msg); }
    public void error(String tag, String msg) { log(Level.ERROR, tag, msg); }

    public void error(String tag, String msg, Throwable t) {
        log(Level.ERROR, tag, msg + " | " + t.getClass().getSimpleName() + ": " + t.getMessage());
    }

    private void log(Level level, String tag, String msg) {
        if (level.ordinal() < minLevel.ordinal()) return;
        String timestamp = LocalDateTime.now().format(TIME_FMT);
        String line = String.format("[%s] [%s] [%s] %s", timestamp, level.name(), tag, msg);
        if (consoleEnabled) {
            if (level == Level.ERROR) System.err.println(line);
            else System.out.println(line);
        }
        if (fileWriter != null) fileWriter.println(line);
    }

    public void close() {
        if (fileWriter != null) {
            info("GameLogger", "Logger shutting down");
            fileWriter.flush();
            fileWriter.close();
        }
    }
}
