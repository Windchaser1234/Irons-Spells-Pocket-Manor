package com.windchaser.pocketmanor.manor;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Per-world persistent state for the shared manor.
 *
 * <p>Records whether the manor has already been built and, once it has, the world position a
 * player should arrive at (the "entrance"). The entrance is resolved at build time - for a
 * player-designed structure it comes from a marker block in the build - so it is stored here
 * rather than hard-coded.
 *
 * <p>Stored on the manor {@link ServerLevel}'s data storage, so it is saved with the world and
 * loaded back on restart. Together with the fact that the manor is a single global dimension,
 * this is what enforces "one manor per world".
 */
public class ManorSavedData extends SavedData {
    private static final String DATA_NAME = "pocketmanor_manor";

    public boolean built = false;
    public boolean hasEntrance = false;
    public double entranceX;
    public double entranceY;
    public double entranceZ;

    public static ManorSavedData get(ServerLevel manorLevel) {
        return manorLevel.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(ManorSavedData::new, ManorSavedData::load),
                DATA_NAME);
    }

    public static ManorSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        ManorSavedData data = new ManorSavedData();
        data.built = tag.getBoolean("built");
        data.hasEntrance = tag.getBoolean("hasEntrance");
        data.entranceX = tag.getDouble("entranceX");
        data.entranceY = tag.getDouble("entranceY");
        data.entranceZ = tag.getDouble("entranceZ");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putBoolean("built", built);
        tag.putBoolean("hasEntrance", hasEntrance);
        tag.putDouble("entranceX", entranceX);
        tag.putDouble("entranceY", entranceY);
        tag.putDouble("entranceZ", entranceZ);
        return tag;
    }
}
