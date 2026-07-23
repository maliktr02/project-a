package com.projecta;

import java.awt.Color;

public class ObjectConfig {
    public final String id;
    public final int level; // 1 to 12
    public final String nameKey;
    public final long value;
    public final double mass;
    public final long score;
    public final Color color;
    public final double radius;

    public ObjectConfig(String id, int level, String nameKey, long value, double mass, long score, Color color, double radius) {
        this.id = id;
        this.level = level;
        this.nameKey = nameKey;
        this.value = value;
        this.mass = mass;
        this.score = score;
        this.color = color;
        this.radius = radius;
    }
}
