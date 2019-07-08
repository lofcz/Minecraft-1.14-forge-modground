package com.example.examplemod.util;

import com.example.examplemod.ConfigManager;
import com.example.examplemod.ExampleMod;
import com.example.examplemod.event.RenderChunkUniformsEvent;
import net.minecraft.client.renderer.chunk.ChunkRender;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType;
import net.minecraftforge.common.MinecraftForge;

public class RenderUtil {
    public static boolean lightingEnabled = false;
    public static ShaderManager previousShader;
    public static boolean enabledLast = false;
    public static ItemCameraTransforms.TransformType itemTransformType = TransformType.NONE;

    public static void renderChunkUniforms(ChunkRender c) {
        MinecraftForge.EVENT_BUS.post(new RenderChunkUniformsEvent(c));
    }

    public static void enableLightingUniforms() {
        if (!ExampleMod.isGui && ConfigManager.isLightingEnabled()) {
            if (enabledLast) {
                if (previousShader != null)
                    previousShader.useShader();
                enabledLast = false;
            }
            if (ShaderManager.isCurrentShader(ShaderUtil.entityLightProgram)) {
                ShaderUtil.entityLightProgram.setUniform("lightingEnabled", true);
            }
        }
    }

    public static void disableLightingUniforms() {
        if (!ExampleMod.isGui && ConfigManager.isLightingEnabled()) {
            if (ShaderManager.isCurrentShader(ShaderUtil.entityLightProgram)) {
                ShaderUtil.entityLightProgram.setUniform("lightingEnabled", false);
            }
            if (!enabledLast) {
                previousShader = ShaderManager.getCurrentShader();
                enabledLast = true;
                ShaderManager.stopShader();
            }
        }
    }

    public static void setTransform(TransformType t) {
        itemTransformType = t;
    }

}
