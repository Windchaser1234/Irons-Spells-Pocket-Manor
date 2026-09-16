package com.windchaser.pocketmanor.registry;

import com.windchaser.pocketmanor.PocketManor;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Keys for the custom "manor" dimension. The dimension itself is defined by the datapack
 * JSON files under {@code data/pocketmanor/dimension/manor.json} and
 * {@code data/pocketmanor/dimension_type/manor.json}, which ship inside this mod's jar.
 *
 * <p>A dimension is world-global, so referencing this single key everywhere is what makes the
 * manor shared: one manor per world, not one per player.
 */
public final class PMDimensions {
    public static final ResourceKey<Level> MANOR = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(PocketManor.MOD_ID, "manor"));

    private PMDimensions() {
    }
}
