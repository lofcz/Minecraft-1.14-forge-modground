package com.example.examplemod.event;

import com.example.examplemod.lighting.Light;
import com.google.common.collect.ImmutableList;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.eventbus.api.Event;

import java.util.ArrayList;

public class GatherLightsEvent extends Event {
    private final ArrayList<Light> lights;
    private final float maxDistance;
    private final Vec3d cameraPosition;
    private final ICamera camera;

    public GatherLightsEvent(ArrayList<Light> lights, float maxDistance, Vec3d cameraPosition, ICamera camera) {
        this.lights = lights;
        this.maxDistance = maxDistance;
        this.cameraPosition = cameraPosition;
        this.camera = camera;
    }

    public ImmutableList<Light> getLightList() {
        return ImmutableList.copyOf(lights);
    }

    public float getMaxDistance() {
        return maxDistance;
    }

    public Vec3d getCameraPosition() {
        return cameraPosition;
    }

    public ICamera getCamera() {
        return camera;
    }

    public void add(Light light) {
        float radius = light.radius();
        if (cameraPosition != null) {
            double dist = MathHelper.sqrt(cameraPosition.squareDistanceTo(light.x, light.y, light.z));
            if (dist > radius + maxDistance) {
                return;
            }
        }

        if (camera != null && !camera.isBoundingBoxInFrustum(new AxisAlignedBB(
                light.x - radius,
                light.y - radius,
                light.z - radius,
                light.x + radius,
                light.y + radius,
                light.z + radius
        ))) {
            return;
        }
        lights.add(light);
    }

    @Override
    public boolean isCancelable() {
        return false;
    }
}
