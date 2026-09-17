package com.windchaser.pocketmanor.manor;

import com.windchaser.pocketmanor.registry.PMDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Handles moving a player into and out of the shared manor dimension.
 *
 * <p>Casting the spell "toggles": if you are not in the manor it stores where you are and sends
 * you in; if you are in the manor it sends you back to your stored location. The stored return
 * point lives in the player's persisted NBT so it survives relogging and death.
 */
public final class ManorManager {
    /** Sub-tag key inside the vanilla {@link Player#PERSISTED_NBT_TAG} compound. */
    private static final String KEY = "pocketmanor";

    private ManorManager() {
    }

    public static void teleportToggle(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        ServerLevel manorLevel = server.getLevel(PMDimensions.MANOR);
        if (manorLevel == null) {
            // The datapack dimension failed to load for some reason.
            player.displayClientMessage(
                    Component.translatable("ui.pocketmanor.no_dimension").withStyle(ChatFormatting.RED), true);
            return;
        }

        // Build the manor exactly once for this world.
        ManorBuilder.ensureBuilt(manorLevel);

        CompoundTag data = getData(player);
        boolean inManor = player.level().dimension().equals(PMDimensions.MANOR);

        if (inManor && data.getBoolean("hasReturn")) {
            // ---- Leave the manor: go back to the stored location ----
            ServerLevel destination = resolveLevel(server, data.getString("dim"));
            Vec3 pos = new Vec3(data.getDouble("x"), data.getDouble("y"), data.getDouble("z"));
            float yaw = data.getFloat("yaw");
            float pitch = data.getFloat("pitch");

            data.putBoolean("hasReturn", false);
            setData(player, data);

            player.changeDimension(new DimensionTransition(
                    destination, pos, Vec3.ZERO, yaw, pitch, DimensionTransition.DO_NOTHING));
        } else if (!inManor) {
            // ---- Enter the manor: remember where we came from ----
            data.putBoolean("hasReturn", true);
            data.putString("dim", player.level().dimension().location().toString());
            data.putDouble("x", player.getX());
            data.putDouble("y", player.getY());
            data.putDouble("z", player.getZ());
            data.putFloat("yaw", player.getYRot());
            data.putFloat("pitch", player.getXRot());
            setData(player, data);

            player.changeDimension(new DimensionTransition(
                    manorLevel, ManorBuilder.entrancePos(manorLevel), Vec3.ZERO,
                    180f, 0f, DimensionTransition.DO_NOTHING));
        } else {
            // In the manor but no stored return (e.g. died inside): fall back to overworld spawn.
            ServerLevel overworld = server.overworld();
            BlockPos spawn = overworld.getSharedSpawnPos();
            player.changeDimension(new DimensionTransition(
                    overworld, Vec3.atBottomCenterOf(spawn), Vec3.ZERO,
                    player.getYRot(), player.getXRot(), DimensionTransition.DO_NOTHING));
        }
    }

    private static ServerLevel resolveLevel(MinecraftServer server, String dimId) {
        if (dimId == null || dimId.isEmpty()) {
            return server.overworld();
        }
        ResourceLocation loc = ResourceLocation.tryParse(dimId);
        if (loc == null) {
            return server.overworld();
        }
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, loc));
        return level != null ? level : server.overworld();
    }

    // --- Persisted return-point storage -------------------------------------------------------

    private static CompoundTag getData(ServerPlayer player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        return persisted.getCompound(KEY);
    }

    private static void setData(ServerPlayer player, CompoundTag data) {
        CompoundTag root = player.getPersistentData();
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        persisted.put(KEY, data);
        root.put(Player.PERSISTED_NBT_TAG, persisted);
    }
}
