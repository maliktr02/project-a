package com.projecta;

import java.util.List;

public class DangerSystem {

    private final double maxDangerTime;
    private double currentDangerTime;
    private boolean isDangerActive = false;
    private boolean isGameOver = false;
    private double graceTimer = 0.0;
    private static final double GRACE_PERIOD = 1.5;
    private static final double SETTLE_VELOCITY_THRESHOLD = 25.0;
    private static final double MIN_TIME_IN_PLAY = 2.0;
    private double objectSettleAccumulator = 0.0;

    public DangerSystem(double maxDangerTime) {
        this.maxDangerTime = maxDangerTime;
        this.currentDangerTime = maxDangerTime;
    }

    public void update(List<GameObject> objects, double dangerY, double dt) {
        if (isGameOver) return;

        graceTimer = Math.max(0, graceTimer - dt);

        boolean objectInDanger = false;
        int settledAboveLine = 0;

        for (GameObject obj : objects) {
            if (!obj.isDropping) continue;
            if (obj.y - obj.radius < dangerY) {
                if (obj.hasCollided && Math.abs(obj.vy) < 15.0) {
                    settledAboveLine++;
                }
            }
        }

        if (settledAboveLine > 0 && graceTimer <= 0) {
            objectSettleAccumulator += dt;
            if (objectSettleAccumulator >= MIN_TIME_IN_PLAY) {
                objectInDanger = true;
            }
        } else {
            objectSettleAccumulator = Math.max(0, objectSettleAccumulator - dt * 3.0);
        }

        isDangerActive = objectInDanger;

        if (isDangerActive) {
            currentDangerTime -= dt;
            if (currentDangerTime <= 0) {
                currentDangerTime = 0;
                isGameOver = true;
            }
        } else {
            if (currentDangerTime < maxDangerTime) {
                currentDangerTime += dt * 0.8;
                if (currentDangerTime > maxDangerTime) {
                    currentDangerTime = maxDangerTime;
                }
            }
        }
    }

    public void onObjectDropped() {
        graceTimer = GRACE_PERIOD;
    }

    public void reset() {
        currentDangerTime = maxDangerTime;
        isDangerActive = false;
        isGameOver = false;
        graceTimer = 0;
        objectSettleAccumulator = 0;
    }

    public boolean isDangerActive() { return isDangerActive; }
    public boolean isGameOver() { return isGameOver; }
    public double getCurrentDangerTime() { return currentDangerTime; }
    public double getMaxDangerTime() { return maxDangerTime; }
    public double getDangerProgress() { return 1.0 - (currentDangerTime / maxDangerTime); }
}
