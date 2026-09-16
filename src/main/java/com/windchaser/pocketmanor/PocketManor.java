package com.windchaser.pocketmanor;

import com.mojang.logging.LogUtils;
import com.windchaser.pocketmanor.registry.PMSpells;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Pocket Manor - an add-on for Iron's Spells 'n Spellbooks (Minecraft 1.21.1 / NeoForge).
 *
 * <p>Adds the "Pocket Manor" spell: an upgraded pocket dimension that teleports the caster
 * into a manor located in its own dedicated dimension. Because the manor lives in a single,
 * shared dimension there is exactly one manor per world - every player who casts the spell is
 * sent to the same building (as opposed to each player getting their own instanced pocket).
 */
@Mod(PocketManor.MOD_ID)
public class PocketManor {
    public static final String MOD_ID = "pocketmanor";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PocketManor(IEventBus modEventBus, ModContainer modContainer) {
        // Register our spell(s) into Iron's Spells' spell registry.
        PMSpells.register(modEventBus);
        LOGGER.info("[Pocket Manor] Registered {} spell(s) for Iron's Spells 'n Spellbooks.", 1);
    }
}
