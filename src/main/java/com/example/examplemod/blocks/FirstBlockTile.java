package com.example.examplemod.blocks;

import com.example.examplemod.tools.CustomEnergyStorage;
import net.minecraft.client.renderer.texture.ITickable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FirstBlockTile extends TileEntity implements ITickableTileEntity, INamedContainerProvider {

    private LazyOptional<IItemHandler> handler = LazyOptional.of(this::createHandler);
    private LazyOptional<IEnergyStorage> energy = LazyOptional.of(this::createEnergy);

    public FirstBlockTile() {
        super(ModBlocks.FIRSTBLOCK_TILE);
    }

    private int counter;

    @Override
    public void tick() {
        if (world.isRemote) {
        }

        if (counter > 0) {
            counter--;

            if (counter <= 0) {
                energy.ifPresent(e -> ((CustomEnergyStorage)e).addEnergy(1000));
            }
            markDirty();
        }
        else {
            handler.ifPresent(h -> {
                ItemStack stack = h.getStackInSlot(0);

                if (stack.getItem() == Items.DIAMOND) {
                    h.extractItem(0, 1, false);
                    counter = 20;
                    markDirty();
                }
            });
        }

        sendOutPower();
    }

    private void sendOutPower() {

    }

    @Override
    public void read(CompoundNBT tag) {
        CompoundNBT invTag = tag.getCompound("inv");
        handler.ifPresent(h -> ((INBTSerializable<CompoundNBT>)h).deserializeNBT(invTag));

        CompoundNBT energyTag = tag.getCompound("energy");
        energy.ifPresent(h -> ((INBTSerializable<CompoundNBT>)h).deserializeNBT(energyTag));

        //energy.ifPresent(h -> ((CustomEnergyStorage)h).setEnergy(invTag.getInt("energy")));

        super.read(tag);
    }

    @Override
    public CompoundNBT write(CompoundNBT tag) {

        handler.ifPresent(h -> {
            CompoundNBT compound = ((INBTSerializable<CompoundNBT>)h).serializeNBT();
            tag.put("inv", compound);
        });

        energy.ifPresent(h -> {
            //tag.putInt("energy", h.getEnergyStored());

            CompoundNBT compound = ((INBTSerializable<CompoundNBT>)h).serializeNBT();
            tag.put("energy", compound);
        });

        return super.write(tag);
    }

    private IEnergyStorage createEnergy() {
        return new CustomEnergyStorage(100000, 0);
    }

    private IItemHandler createHandler() {

        return new ItemStackHandler(1) {


                // fix potential wrong save
                @Override
                protected void onContentsChanged(int slot) {
                    markDirty(); // forces save
                }

                @Override
                public boolean isItemValid(int slot, @Nonnull ItemStack stack) {

                    return stack.getItem() == Items.DIAMOND;
                    //return super.isItemValid(slot, stack);
                }

                @Nonnull
                @Override
                public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {

                    if (stack.getItem() != Items.DIAMOND) {
                        return stack;
                    }

                    return super.insertItem(slot, stack, simulate);
                }
            };
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {

        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY){
            return handler.cast();
        }

        if (cap == CapabilityEnergy.ENERGY) {
            return energy.cast();
        }

        return super.getCapability(cap, side);
    }

    @Override
    public ITextComponent getDisplayName() {
        return new StringTextComponent(getType().getRegistryName().getPath());
    }

    @Nullable
    @Override
    public Container createMenu(int i, PlayerInventory playerInventory, PlayerEntity playerEntity) {
        return new FirstBlockContainer(i, world, pos, playerInventory, playerEntity);
    }
}
