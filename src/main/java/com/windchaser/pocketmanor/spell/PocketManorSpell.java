package com.windchaser.pocketmanor.spell;

import com.windchaser.pocketmanor.PocketManor;
import com.windchaser.pocketmanor.manor.ManorManager;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * The Pocket Manor spell.
 *
 * <p>An upgraded, higher-tier cousin of a simple pocket-dimension teleport. Casting it while in
 * the overworld (or anywhere that is not the manor) records your current location and warps you
 * into the shared manor dimension. Casting it again from inside the manor sends you back to where
 * you came from. The manor is a single, world-global build - everyone shares it.
 *
 * <p>It is an ENDER-school, LONG (channeled) cast so it feels weightier/"upgraded" compared to the
 * base teleport, and it is gated behind a high mana cost and EPIC rarity.
 */
public class PocketManorSpell extends AbstractSpell {
    private final ResourceLocation spellId =
            ResourceLocation.fromNamespaceAndPath(PocketManor.MOD_ID, "pocket_manor");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(30)
            .build();

    public PocketManorSpell() {
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
        this.castTime = 60; // 3 second channel - slower/grander than the basic teleport
        this.baseManaCost = 300;
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.BEACON_ACTIVATE);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.ENDERMAN_TELEPORT);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.pocketmanor.pocket_manor.info").withStyle(ChatFormatting.GRAY),
                Component.translatable("ui.pocketmanor.pocket_manor.info2").withStyle(ChatFormatting.GRAY)
        );
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (entity instanceof ServerPlayer serverPlayer) {
            ManorManager.teleportToggle(serverPlayer);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
