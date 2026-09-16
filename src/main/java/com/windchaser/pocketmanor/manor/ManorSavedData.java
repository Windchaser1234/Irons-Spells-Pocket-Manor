package com.windchaser.pocketmanor.manor;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Per-world persistent flag that records whether the shared manor has already been built.
 *
 * <p>Stored on the manor {@link ServerLevel}'s data storage, so it is saved with the world and
 * loaded back on restart. This is what stops the manor from being regenerated every time a player
 * teleports in, and - together with the fact that the manor is a single global dimension - is what
 * enforces "one manor per world".
 */
public class ManorSavedData extends SavedData {
    private static final String DATA_NAME = "pocketmanor_manor";

    public boolean built = false;

    public static ManorSavedData get(ServerLevel manorLevel) {
        return manorLevel.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(ManorSavedData::new, ManorSavedData::load),
                DATA_NAME);
    }

    public static ManorSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        ManorSavedData data = new ManorSavedData();
        data.built = tag.getBoolean("built");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putBoolean("built", built);
        return tag;
    }
}
