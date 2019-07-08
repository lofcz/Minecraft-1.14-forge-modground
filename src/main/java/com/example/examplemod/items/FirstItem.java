package com.example.examplemod.items;

import com.example.examplemod.ExampleMod;
import net.minecraft.item.Item;

public class FirstItem extends Item {

    public FirstItem() {
        super(new Item.Properties()
                .maxStackSize(4)
                .group(ExampleMod.setup.itemGroup));

        setRegistryName("firstitem");
    }
}
