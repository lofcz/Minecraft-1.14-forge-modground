package com.example.examplemod.lighting;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class Light {
    public float x;
    public float y;
    public float z;
    public float r;
    public float g;
    public float b;
    public float a;
    public float rx;
    public float ry;
    public float rz;
    public float angle;

    public Light(float x, float y, float z, float r, float g, float b, float a, float radius) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        this.rx = 0;
        this.ry = radius;
        this.rz = 0;
    }

    public Light(float x, float y, float z, float r, float g, float b, float a, float rx, float ry, float rz, float angle) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        this.rx = rx;
        this.ry = ry;
        this.rz = rz;
        this.angle = angle;
    }

    public static Builder builder() {
        return new Builder();
    }

    public float radius() {
        return (float) Math.sqrt(rx * rx + ry * ry + rz * rz);
    }

    public static final class Builder {
        private float x = Float.NaN;
        private float y = Float.NaN;
        private float z = Float.NaN;

        private float r = Float.NaN;
        private float g = Float.NaN;
        private float b = Float.NaN;
        private float a = Float.NaN;

        private float rx = Float.NaN;
        private float ry = Float.NaN;
        private float rz = Float.NaN;

        private float angle = Float.NaN;


        public Builder pos(BlockPos pos) {
            return pos(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f);
        }

        public Builder pos(Vec3d pos) {
            return pos(pos.x, pos.y, pos.z);
        }

        public Builder pos(Entity e) {
            return pos(e.posX, e.posY, e.posZ);
        }

        public Builder pos(double x, double y, double z) {
            return pos((float) x, (float) y, (float) z);
        }

        public Builder pos(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }


        public Builder color(int c, boolean hasAlpha) {
            return color(extract(c, 2), extract(c, 1), extract(c, 0), hasAlpha ? extract(c, 3) : 1);
        }

        private float extract(int i, int idx) {
            return ((i >> (idx * 8)) & 0xFF) / 255f;
        }

        public Builder color(float r, float g, float b) {
            return color(r, g, b, 1f);
        }

        public Builder color(float r, float g, float b, float a) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            return this;
        }

        public Builder radius(float radius) {
            this.rx = 0;
            this.ry = radius;
            this.rz = 0;
            this.angle = (float) (Math.PI * 2.0);
            return this;
        }

        public Builder direction(float x, float y, float z, float angle) {
            this.rx = x;
            this.ry = y;
            this.rz = z;
            this.angle = angle;
            return this;
        }

        public Builder direction(Vec3d vec, float angle) {
            this.rx = (float) vec.x;
            this.ry = (float) vec.y;
            this.rz = (float) vec.z;
            this.angle = angle;
            return this;
        }

        public Light build() {
            if (Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z) &&
                    Float.isFinite(r) && Float.isFinite(g) && Float.isFinite(b) && Float.isFinite(a) &&
                    Float.isFinite(rx) && Float.isFinite(ry) && Float.isFinite(rz) && Float.isFinite(angle)) {
                return new Light(x, y, z, r, g, b, a, rx, ry, rz, angle);
            } else {
                throw new IllegalArgumentException("Position, color, and radius must be set, and cannot be infinite");
            }
        }
    }
}
