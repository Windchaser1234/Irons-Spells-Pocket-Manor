package com.windchaser.pocketmanor.registry;

import com.windchaser.pocketmanor.PocketManor;
import com.windchaser.pocketmanor.spell.PocketManorSpell;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Our own {@link DeferredRegister} that hangs off Iron's Spells' spell registry key.
 * This is exactly the pattern the base mod uses internally - see
 * {@code io.redspace.ironsspellbooks.api.registry.SpellRegistry}.
 */
public class PMSpells {
    public static final DeferredRegister<AbstractSpell> SPELLS =
            DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, PocketManor.MOD_ID);

    public static final DeferredHolder<AbstractSpell, PocketManorSpell> POCKET_MANOR =
            SPELLS.register("pocket_manor", PocketManorSpell::new);

    public static void register(IEventBus modEventBus) {
        SPELLS.register(modEventBus);
    }

    private PMSpells() {
    }
}
