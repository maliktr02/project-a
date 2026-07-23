package com.projecta;

import java.util.List;

public class PhysicsEngine {

    private double gravity;
    private double bounceDamping;
    private double friction;
    private double airDrag;
    private int subSteps;

    public PhysicsEngine(double gravity, double bounceDamping) {
        this.gravity = gravity * 85.0;
        this.bounceDamping = bounceDamping;
        this.friction = 0.97;
        this.airDrag = 0.999;
        this.subSteps = 8;
    }

    public void configure(double gravity, double bounceDamping, double friction, double airDrag, int subSteps) {
        this.gravity = gravity * 85.0;
        this.bounceDamping = bounceDamping;
        this.friction = friction;
        this.airDrag = airDrag;
        this.subSteps = Math.max(1, Math.min(20, subSteps));
    }

    public double getGravityRaw() { return gravity / 85.0; }
    public double getBounceDamping() { return bounceDamping; }
    public double getFriction() { return friction; }
    public double getAirDrag() { return airDrag; }
    public int getSubSteps() { return subSteps; }

    public void update(List<GameObject> objects, double dt, double boundsLeft, double boundsRight, double boundsBottom) {
        double subDt = dt / subSteps;

        for (int step = 0; step < subSteps; step++) {
            for (GameObject obj : objects) {
                if (!obj.isDropping) continue;
                obj.vy += gravity * subDt;
                obj.vx *= Math.pow(airDrag, subDt * 60.0);
                obj.x += obj.vx * subDt;
                obj.y += obj.vy * subDt;
            }

            for (int i = 0; i < objects.size(); i++) {
                GameObject a = objects.get(i);
                if (!a.isDropping) continue;
                for (int j = i + 1; j < objects.size(); j++) {
                    GameObject b = objects.get(j);
                    if (!b.isDropping) continue;
                    resolveCircleCollision(a, b);
                }
            }

            for (GameObject obj : objects) {
                if (!obj.isDropping) continue;
                resolveWallCollision(obj, boundsLeft, boundsRight, boundsBottom);
            }
        }
    }

    private void resolveCircleCollision(GameObject a, GameObject b) {
        double dx = b.x - a.x;
        double dy = b.y - a.y;
        double distSq = dx * dx + dy * dy;
        double minDist = a.radius + b.radius;

        if (distSq <= 0.0001) { dx = 0.1; dy = 0.0; distSq = 0.01; }

        if (distSq < minDist * minDist) {
            double dist = Math.sqrt(distSq);
            double overlap = minDist - dist;

            double nx = dx / dist;
            double ny = dy / dist;

            double totalMass = a.mass + b.mass;
            double aRatio = b.mass / totalMass;
            double bRatio = a.mass / totalMass;

            a.x -= nx * overlap * aRatio;
            a.y -= ny * overlap * aRatio;
            b.x += nx * overlap * bRatio;
            b.y += ny * overlap * bRatio;

            double rvx = b.vx - a.vx;
            double rvy = b.vy - a.vy;
            double velAlongNormal = rvx * nx + rvy * ny;

            if (velAlongNormal < 0) {
                double restitution = bounceDamping;
                double impulseScalar = -(1.0 + restitution) * velAlongNormal / (1.0 / a.mass + 1.0 / b.mass);

                double impulseX = impulseScalar * nx;
                double impulseY = impulseScalar * ny;

                a.vx -= impulseX / a.mass;
                a.vy -= impulseY / a.mass;
                b.vx += impulseX / b.mass;
                b.vy += impulseY / b.mass;

                double tx = -ny;
                double ty = nx;
                double velAlongTangent = rvx * tx + rvy * ty;
                double maxFrictionImpulse = Math.abs(impulseScalar) * 0.3;
                double tangentImpulse = Math.max(-maxFrictionImpulse, Math.min(maxFrictionImpulse, -velAlongTangent * 0.15));

                a.vx -= tangentImpulse * tx / a.mass;
                a.vy -= tangentImpulse * ty / a.mass;
                b.vx += tangentImpulse * tx / b.mass;
                b.vy += tangentImpulse * ty / b.mass;

                a.angularVelocity += tangentImpulse / a.radius * 0.5;
                b.angularVelocity -= tangentImpulse / b.radius * 0.5;

                double impactForce = Math.abs(velAlongNormal);
                if (impactForce > 50) {
                    double squishAmount = Math.min(0.2, impactForce / 1200.0);
                    double hitAngle = Math.atan2(ny, nx);
                    a.squishX = 1.0 + squishAmount * Math.abs(Math.cos(hitAngle));
                    a.squishY = 1.0 - squishAmount * Math.abs(Math.cos(hitAngle));
                    b.squishX = 1.0 - squishAmount * Math.abs(Math.cos(hitAngle));
                    b.squishY = 1.0 + squishAmount * Math.abs(Math.cos(hitAngle));
                }
            }
        }
    }

    private void resolveWallCollision(GameObject obj, double left, double right, double bottom) {
        if (obj.x - obj.radius < left) {
            obj.x = left + obj.radius;
            obj.vx = Math.abs(obj.vx) * bounceDamping;
            obj.angularVelocity += obj.vy * 0.03;
        }

        if (obj.x + obj.radius > right) {
            obj.x = right - obj.radius;
            obj.vx = -Math.abs(obj.vx) * bounceDamping;
            obj.angularVelocity -= obj.vy * 0.03;
        }

        if (obj.y + obj.radius > bottom) {
            obj.y = bottom - obj.radius;
            obj.vy = -Math.abs(obj.vy) * bounceDamping;
            obj.vx *= friction;
            if (Math.abs(obj.vy) < 10.0) obj.vy = 0;
            obj.angularVelocity *= 0.92;
        }
    }
}
