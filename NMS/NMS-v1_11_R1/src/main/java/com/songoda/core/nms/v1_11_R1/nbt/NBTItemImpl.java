package com.songoda.core.nms.v1_11_R1.nbt;

import com.songoda.core.nms.nbt.NBTCompound;
import com.songoda.core.nms.nbt.NBTItem;
import net.minecraft.server.v1_11_R1.NBTTagCompound;
import org.bukkit.craftbukkit.v1_11_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

public class NBTItemImpl extends NBTCompoundImpl implements NBTItem {
    private final net.minecraft.server.v1_11_R1.ItemStack nmsItem;

    public NBTItemImpl(net.minecraft.server.v1_11_R1.ItemStack nmsItem) {
        super(nmsItem != null && nmsItem.hasTag() ? nmsItem.getTag() : new NBTTagCompound());

        this.nmsItem = nmsItem;
    }

    public ItemStack finish() {
        if (nmsItem == null) {
            return CraftItemStack.asBukkitCopy(new net.minecraft.server.v1_11_R1.ItemStack(compound));
        }

        return CraftItemStack.asBukkitCopy(nmsItem);
    }

    @Override
    public NBTCompound set(String tag, byte[] b) {
        compound.setByteArray(tag, b);
        return this;
    }

    @Override
    public byte[] getByteArray(String tag) {
        return new byte[0];
    }
}
