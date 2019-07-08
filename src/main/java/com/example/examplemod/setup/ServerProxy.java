package com.example.examplemod.setup;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

public class ServerProxy implements IProxy {

    @Override
    public World getClientWorld() {
        throw new IllegalStateException("This mod can run only at client");
    }

    @Override
    public void init() {

    }

    @Override
    public PlayerEntity getClientPlayer() {
        throw new IllegalStateException("This mod can run only at client");
    }
}
