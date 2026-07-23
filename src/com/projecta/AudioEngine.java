package com.projecta;

import javax.sound.sampled.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AudioEngine {

    private static final int SAMPLE_RATE = 22050;
    private int sfxVolumePercent = 80;
    private int musicVolumePercent = 70;
    private boolean sfxEnabled = true;
    private boolean musicEnabled = true;

    private Clip bgmClip;
    private Clip introClip;
    private File audioDir;
    private final Map<String, File> audioFiles = new HashMap<>();

    public AudioEngine() {}

    public void init(File rootDir) {
        audioDir = new File(rootDir, "audio");
        scanAudioFiles();
        GameLogger.get().info("AudioEngine", "Audio dir: " + audioDir.getAbsolutePath() + " | Files found: " + audioFiles.size());
    }

    private void scanAudioFiles() {
        if (!audioDir.exists()) return;
        scanDir(audioDir, "");
    }

    private void scanDir(File dir, String prefix) {
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File f : children) {
            if (f.isDirectory()) {
                scanDir(f, prefix + f.getName() + "/");
            } else {
                String name = f.getName().toLowerCase();
                if (name.endsWith(".wav") || name.endsWith(".aiff") || name.endsWith(".au") || name.endsWith(".mp3") || name.endsWith(".ogg")) {
                    String key = prefix + f.getName().substring(0, f.getName().lastIndexOf('.'));
                    audioFiles.put(key.toLowerCase(), f);
                }
            }
        }
    }

    public void setVolumes(int sfxVol, int musicVol, boolean sfxOn, boolean musicOn) {
        this.sfxVolumePercent = Math.min(100, Math.max(0, sfxVol));
        this.musicVolumePercent = Math.min(100, Math.max(0, musicVol));
        this.sfxEnabled = sfxOn;
        this.musicEnabled = musicOn;

        if (bgmClip != null && bgmClip.isOpen()) {
            setClipVolume(bgmClip, musicVolumePercent / 100.0f);
            if (!musicEnabled && bgmClip.isRunning()) bgmClip.stop();
            else if (musicEnabled && !bgmClip.isRunning()) {
                bgmClip.start();
                bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
            }
        }
    }

    public Clip playIntroSound() {
        File introFile = findAudioFile("intro");
        if (introFile != null) {
            introClip = playFile(introFile, sfxVolumePercent / 100.0f, false);
            return introClip;
        }
        return null;
    }

    public void playDropSound() {
        if (!sfxEnabled || sfxVolumePercent <= 0) return;
        File f = findAudioFile("sfx/drop");
        if (f != null) { playFileAsync(f, sfxVolumePercent / 100.0f); return; }
        new Thread(() -> playPcm(generateToneSweep(180, 70, 0.08, 0.4), sfxVolumePercent / 100.0f)).start();
    }

    public void playMergeSound(int level) {
        if (!sfxEnabled || sfxVolumePercent <= 0) return;
        File f = findAudioFile("sfx/merge_" + level);
        if (f == null) f = findAudioFile("sfx/merge");
        if (f != null) { playFileAsync(f, sfxVolumePercent / 100.0f); return; }
        new Thread(() -> {
            double baseFreq = 300 + level * 65;
            playPcm(generateToneSweep(baseFreq, baseFreq * 1.5, 0.15, 0.5), sfxVolumePercent / 100.0f);
        }).start();
    }

    public void playComboSound(int comboCount) {
        if (!sfxEnabled || sfxVolumePercent <= 0) return;
        File f = findAudioFile("sfx/combo");
        if (f != null) { playFileAsync(f, sfxVolumePercent / 100.0f); return; }
        new Thread(() -> {
            double freq = 500 + comboCount * 120;
            playPcm(generateToneSweep(freq, freq * 1.25, 0.2, 0.6), sfxVolumePercent / 100.0f);
        }).start();
    }

    public void playButtonClickSound() {
        if (!sfxEnabled || sfxVolumePercent <= 0) return;
        File f = findAudioFile("sfx/click");
        if (f != null) { playFileAsync(f, sfxVolumePercent / 100.0f); return; }
        new Thread(() -> playPcm(generateToneSweep(600, 300, 0.04, 0.25), sfxVolumePercent / 100.0f)).start();
    }

    public void playGameOverSound() {
        if (!sfxEnabled || sfxVolumePercent <= 0) return;
        File f = findAudioFile("sfx/gameover");
        if (f != null) { playFileAsync(f, sfxVolumePercent / 100.0f); return; }
        new Thread(() -> playPcm(generateToneSweep(400, 100, 0.5, 0.6), sfxVolumePercent / 100.0f)).start();
    }

    public void playDangerSound() {
        if (!sfxEnabled || sfxVolumePercent <= 0) return;
        File f = findAudioFile("sfx/danger");
        if (f != null) { playFileAsync(f, sfxVolumePercent / 100.0f); return; }
        new Thread(() -> playPcm(generateToneSweep(150, 120, 0.1, 0.3), sfxVolumePercent / 100.0f)).start();
    }

    public void playTauntSound() {
        if (!sfxEnabled || sfxVolumePercent <= 0) return;
        File f = findAudioFile("sfx/taunt");
        if (f != null) playFileAsync(f, sfxVolumePercent / 100.0f);
    }

    public void startBackgroundMusic() {
        File bgmFile = findAudioFile("music/bgm");
        if (bgmFile == null) bgmFile = findAudioFile("music/background");
        if (bgmFile == null) bgmFile = findAudioFile("music/theme");

        if (bgmFile != null) {
            bgmClip = playFile(bgmFile, musicVolumePercent / 100.0f, true);
        }
        
        if (bgmClip == null) {
            new Thread(() -> {
                try {
                    byte[] bgmPcm = generateAmbientLoop(8.0);
                    AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
                    bgmClip = AudioSystem.getClip();
                    bgmClip.open(format, bgmPcm, 0, bgmPcm.length);
                    setClipVolume(bgmClip, musicVolumePercent / 100.0f);
                    if (musicEnabled) bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
                } catch (Exception e) {
                    GameLogger.get().error("AudioEngine", "Could not start BGM", e);
                }
            }).start();
        }
    }

    private File findAudioFile(String key) {
        return audioFiles.get(key.toLowerCase());
    }

    private void playFileAsync(File f, float vol) {
        new Thread(() -> playFile(f, vol, false)).start();
    }

    private Clip playFile(File f, float vol, boolean loop) {
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(f);
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            setClipVolume(clip, vol);
            if (loop) clip.loop(Clip.LOOP_CONTINUOUSLY);
            else clip.start();
            return clip;
        } catch (Exception e) {
            GameLogger.get().error("AudioEngine", "Failed to play: " + f.getName(), e);
        }
        return null;
    }

    private byte[] generateToneSweep(double startFreq, double endFreq, double durationSec, double maxVol) {
        int numSamples = (int) (SAMPLE_RATE * durationSec);
        byte[] pcm = new byte[numSamples];
        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double progress = (double) i / numSamples;
            double freq = startFreq + (endFreq - startFreq) * progress;
            double env = Math.sin(progress * Math.PI);
            pcm[i] = (byte) (Math.sin(2.0 * Math.PI * freq * t) * env * maxVol * 127.0);
        }
        return pcm;
    }

    private byte[] generateAmbientLoop(double durationSec) {
        int numSamples = (int) (SAMPLE_RATE * durationSec);
        byte[] pcm = new byte[numSamples];
        double[] chords = {220.0, 261.63, 329.63, 392.0};
        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double sample = 0;
            for (int c = 0; c < chords.length; c++) {
                double lfo = Math.sin(2.0 * Math.PI * 0.2 * t + c);
                sample += Math.sin(2.0 * Math.PI * chords[c] * t) * (0.15 + 0.05 * lfo);
            }
            pcm[i] = (byte) (sample * 0.25 * 127.0);
        }
        return pcm;
    }

    private void playPcm(byte[] pcm, float volumeRatio) {
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
            Clip clip = AudioSystem.getClip();
            clip.open(format, pcm, 0, pcm.length);
            setClipVolume(clip, volumeRatio);
            clip.start();
        } catch (Exception e) {
            GameLogger.get().error("AudioEngine", "Failed to play PCM", e);
        }
    }

    private void setClipVolume(Clip clip, float ratio) {
        try {
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gc = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float dB = (float) (Math.log10(Math.max(0.0001f, ratio)) * 20.0);
                gc.setValue(Math.max(gc.getMinimum(), Math.min(gc.getMaximum(), dB)));
            }
        } catch (Exception ignored) {}
    }

    public void stopAll() {
        if (bgmClip != null && bgmClip.isOpen()) { bgmClip.stop(); bgmClip.close(); }
        if (introClip != null && introClip.isOpen()) { introClip.stop(); introClip.close(); }
    }

    public int getSfxVolume() { return sfxVolumePercent; }
    public int getMusicVolume() { return musicVolumePercent; }
}
