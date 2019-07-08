package com.example.examplemod.blocks;

import net.minecraft.inventory.container.ContainerType;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.registries.ObjectHolder;

public class ModBlocks {

    @ObjectHolder("examplemod:firstblock")
    public static FirstBlock FIRSTBLOCK;

    @ObjectHolder("examplemod:firstblock")
    public static TileEntityType<FirstBlockTile> FIRSTBLOCK_TILE;

    @ObjectHolder("examplemod:firstblock")
    public static ContainerType<FirstBlockContainer> FIRSTBLOCK_CONTAINER;
}
