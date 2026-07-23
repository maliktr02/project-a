package com.projecta;

import java.util.*;

public class MergeSystem {

    public static class MergeEvent {
        public final double x;
        public final double y;
        public final int newLevel;
        public final long scoreGained;
        public final ObjectConfig newConfig;

        public MergeEvent(double x, double y, int newLevel, long scoreGained, ObjectConfig newConfig) {
            this.x = x;
            this.y = y;
            this.newLevel = newLevel;
            this.scoreGained = scoreGained;
            this.newConfig = newConfig;
        }
    }

    private final DataManager dataManager;
    private int comboCount = 0;
    private double comboTimer = 0.0;

    public MergeSystem(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    public void update(double dt) {
        if (comboTimer > 0) {
            comboTimer -= dt;
            if (comboTimer <= 0) {
                comboCount = 0;
            }
        }
    }

    public List<MergeEvent> checkAndPerformMerges(List<GameObject> objects) {
        List<MergeEvent> mergeEvents = new ArrayList<>();
        Set<GameObject> toRemove = new HashSet<>();
        List<GameObject> toAdd = new ArrayList<>();

        for (int i = 0; i < objects.size(); i++) {
            GameObject a = objects.get(i);
            if (!a.isDropping || toRemove.contains(a)) continue;

            for (int j = i + 1; j < objects.size(); j++) {
                GameObject b = objects.get(j);
                if (!b.isDropping || toRemove.contains(b)) continue;

                // Check level match
                if (a.config.level == b.config.level) {
                    double dx = b.x - a.x;
                    double dy = b.y - a.y;
                    double distSq = dx * dx + dy * dy;
                    double mergeDist = a.radius + b.radius + 3.0;

                    if (distSq <= mergeDist * mergeDist) {
                        // Mark originals for removal
                        toRemove.add(a);
                        toRemove.add(b);

                        int nextLevel = Math.min(a.config.level + 1, 12);
                        ObjectConfig nextConfig = dataManager.getObjectConfigByLevel(nextLevel);

                        double avgX = (a.x + b.x) / 2.0;
                        double avgY = (a.y + b.y) / 2.0;

                        GameObject mergedObj = new GameObject(avgX, avgY, nextConfig);
                        mergedObj.isDropping = true;
                        // Give slight upward pop impulse
                        mergedObj.vy = -60.0;
                        toAdd.add(mergedObj);

                        comboCount++;
                        comboTimer = 2.0; // 2 sec combo window

                        long baseScore = nextConfig.score;
                        long finalScore = baseScore * (1 + (comboCount - 1) / 2);

                        mergeEvents.add(new MergeEvent(avgX, avgY, nextLevel, finalScore, nextConfig));
                        break;
                    }
                }
            }
        }

        objects.removeAll(toRemove);
        objects.addAll(toAdd);

        return mergeEvents;
    }

    public int getComboCount() { return comboCount; }
    public void resetCombo() { comboCount = 0; comboTimer = 0; }
}
