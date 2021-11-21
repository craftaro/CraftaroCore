package com.songoda.core.nms.v1_16_R3.world;

import com.songoda.core.compatibility.material.CompatibleMaterial;
import com.songoda.core.compatibility.particle.CompatibleParticleHandler;
import com.songoda.core.nms.world.SSpawner;
import com.songoda.core.nms.world.SpawnedEntity;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.DifficultyDamageScaler;
import net.minecraft.server.v1_16_R3.Entity;
import net.minecraft.server.v1_16_R3.EntityInsentient;
import net.minecraft.server.v1_16_R3.EntityTypes;
import net.minecraft.server.v1_16_R3.EnumMobSpawn;
import net.minecraft.server.v1_16_R3.MobSpawnerData;
import net.minecraft.server.v1_16_R3.NBTTagCompound;
import net.minecraft.server.v1_16_R3.WorldServer;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.Optional;
import java.util.Random;
import java.util.Set;

public class SSpawnerImpl implements SSpawner {
    private final Location spawnerLocation;

    public SSpawnerImpl(Location location) {
        this.spawnerLocation = location;
    }

    @Override
    public LivingEntity spawnEntity(EntityType type, Location spawnerLocation) {
        return spawnEntity(type, "EXPLOSION_NORMAL", null, null);
    }

    @Override
    public LivingEntity spawnEntity(EntityType type, String particleType, SpawnedEntity spawned,
                                    Set<CompatibleMaterial> canSpawnOn) {
        MobSpawnerData data = new MobSpawnerData();
        NBTTagCompound compound = data.getEntity();

        String name = type.name().toLowerCase().replace("snowman", "snow_golem")
                .replace("mushroom_cow", "mooshroom");
        compound.setString("id", "minecraft:" + name);

        short spawnRange = 4;
        for (int i = 0; i < 50; i++) {
            @SuppressWarnings("ConstantConditions")
            WorldServer world = ((CraftWorld) spawnerLocation.getWorld()).getHandle();

            Random random = world.getRandom();
            double x = spawnerLocation.getX() + (random.nextDouble() - random.nextDouble()) * (double) spawnRange + 0.5D;
            double y = spawnerLocation.getY() + random.nextInt(3) - 1;
            double z = spawnerLocation.getZ() + (random.nextDouble() - random.nextDouble()) * (double) spawnRange + 0.5D;

            Optional<Entity> optionalEntity = EntityTypes.a(compound, world);
            if (!optionalEntity.isPresent()) {
                continue;
            }

            Entity entity = optionalEntity.get();
            entity.setPosition(x, y, z);

            BlockPosition position = entity.getChunkCoordinates();
            DifficultyDamageScaler damageScaler = world.getDamageScaler(position);

            if (!(entity instanceof EntityInsentient)) {
                continue;
            }

            EntityInsentient entityInsentient = (EntityInsentient) entity;

            Location spot = new Location(spawnerLocation.getWorld(), x, y, z);
            if (!canSpawn(world, entityInsentient, spot, canSpawnOn)) {
                continue;
            }

            entityInsentient.prepare(world, damageScaler, EnumMobSpawn.SPAWNER, null, null);

            LivingEntity craftEntity = (LivingEntity) entity.getBukkitEntity();

            if (spawned != null && !spawned.onSpawn(craftEntity)) {
                return null;
            }

            if (particleType != null) {
                float xx = (float) (0 + (Math.random() * 1));
                float yy = (float) (0 + (Math.random() * 2));
                float zz = (float) (0 + (Math.random() * 1));
                CompatibleParticleHandler.spawnParticles(CompatibleParticleHandler.ParticleType.getParticle(particleType),
                        spot, 5, xx, yy, zz, 0);
            }

            world.addEntity(entity, CreatureSpawnEvent.SpawnReason.SPAWNER);

            spot.setYaw(random.nextFloat() * 360.0F);
            craftEntity.teleport(spot);

            return craftEntity;
        }

        return null;
    }

    private boolean canSpawn(WorldServer world, EntityInsentient entityInsentient, Location location,
                             Set<CompatibleMaterial> canSpawnOn) {
        if (!world.getCubes(entityInsentient, entityInsentient.getBoundingBox())) {
            return false;
        }

        CompatibleMaterial spawnedIn = CompatibleMaterial.getMaterial(location.getBlock());
        CompatibleMaterial spawnedOn = CompatibleMaterial.getMaterial(location.getBlock().getRelative(BlockFace.DOWN));

        if (spawnedIn == null || spawnedOn == null) {
            return false;
        }

        if (!spawnedIn.isAir() &&
                spawnedIn != CompatibleMaterial.WATER &&
                !spawnedIn.name().contains("PRESSURE") &&
                !spawnedIn.name().contains("SLAB")) {
            return false;
        }

        for (CompatibleMaterial material : canSpawnOn) {
            if (material == null) continue;

            if (spawnedOn.equals(material) || material.isAir()) {
                return true;
            }
        }

        return false;
    }
}
