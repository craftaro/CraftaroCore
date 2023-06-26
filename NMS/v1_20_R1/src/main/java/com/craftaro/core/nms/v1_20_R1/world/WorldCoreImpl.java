package com.craftaro.core.nms.v1_20_R1.world;

import com.craftaro.core.nms.world.*;
import com.craftaro.core.nms.ReflectionUtils;
import com.craftaro.core.nms.world.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.FluidState;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.craftbukkit.v1_20_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_20_R1.block.CraftBlock;
import org.bukkit.inventory.ItemStack;

public class WorldCoreImpl implements WorldCore {
    @Override
    public SSpawner getSpawner(Location location) {
        return new SSpawnerImpl(location);
    }

    @Override
    public SItemStack getItemStack(ItemStack item) {
        return new SItemStackImpl(item);
    }

    @Override
    public SWorld getWorld(World world) {
        return new SWorldImpl(world);
    }

    @Override
    public BBaseSpawner getBaseSpawner(CreatureSpawner spawner) throws NoSuchFieldException, NoSuchMethodException, IllegalAccessException {
        Object cTileEntity = ReflectionUtils.getFieldValue(spawner, "tileEntity");

        return new BBaseSpawnerImpl(spawner, (BaseSpawner) ReflectionUtils.getFieldValue(cTileEntity, "a"));
    }

    /**
     * Method is based on {@link ServerLevel#tickChunk(LevelChunk, int)}.
     */
    @Override
    public void randomTickChunk(org.bukkit.Chunk bukkitChunk, int tickAmount) {
        LevelChunk chunk = (LevelChunk) ((CraftChunk) bukkitChunk).getHandle(ChunkStatus.FULL);
        ServerLevel world = chunk.r;
        ProfilerFiller gameProfilerFiller = world.getProfiler();

        ChunkPos chunkPos = chunk.getPos();
        int j = chunkPos.getMinBlockX();
        int k = chunkPos.getMinBlockZ();

        gameProfilerFiller.push("tickBlocks");
        if (tickAmount > 0) {
            LevelChunkSection[] aChunkSection = chunk.getSections();

            for (int j1 = 0; j1 < aChunkSection.length; ++j1) {
                LevelChunkSection chunkSection = aChunkSection[j1];
                if (chunkSection.isRandomlyTicking()) {
                    int l = chunk.getSectionYFromSectionIndex(j1);
                    int k1 = SectionPos.sectionToBlockCoord(l);

                    for (int i1 = 0; i1 < tickAmount; ++i1) {
                        BlockPos blockposition2 = world.getBlockRandomPos(j, k1, k, 15);
                        gameProfilerFiller.push("randomTick");
                        BlockState iblockdata3 = chunkSection.getBlockState(blockposition2.getX() - j, blockposition2.getY() - k1, blockposition2.getZ() - k);
                        if (iblockdata3.isRandomlyTicking()) {
                            iblockdata3.randomTick(world, blockposition2, world.random);
                        }

                        FluidState fluid = iblockdata3.getFluidState();
                        if (fluid.isRandomlyTicking()) {
                            fluid.randomTick(world, blockposition2, world.random);
                        }

                        gameProfilerFiller.pop();
                    }
                }
            }
        }

        gameProfilerFiller.pop();
    }

    @Override
    public void updateAdjacentComparators(Location location) {
        Block bukkitBlock = location.getBlock();
        CraftBlock craftBlock = (CraftBlock) bukkitBlock;
        ServerLevel serverLevel = craftBlock.getCraftWorld().getHandle();

        serverLevel.updateNeighbourForOutputSignal(craftBlock.getPosition(), craftBlock.getNMS().getBlock());
    }
}
