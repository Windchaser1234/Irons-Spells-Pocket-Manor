# Pocket Manor

An add-on for **[Iron's Spells 'n Spellbooks](https://www.curseforge.com/minecraft/mc-mods/irons-spells-n-spellbooks)**
for **Minecraft 1.21.1 (NeoForge)**.

It adds one spell — **Pocket Manor** — an upgraded take on a pocket-dimension teleport.
Casting it whisks you away to a **grand manor that lives in its own dimension**. Because the
manor is a single, world-global dimension, there is **exactly one manor per world**: every player
who casts the spell arrives in the *same* building and shares it, rather than each player getting
their own private pocket.

Cast the spell again from inside the manor to return to wherever you came from (your position is
remembered across relogs and death).

---

## How it works

| Concern | Approach |
| --- | --- |
| The manor | A custom dimension `pocketmanor:manor`, defined by datapack JSON shipped in the jar (`data/pocketmanor/dimension/manor.json` + `.../dimension_type/manor.json`). It generates as an empty void; the building is placed by code. |
| "One manor per world, shared" | A dimension is world-global, so pointing everyone at the single `pocketmanor:manor` key means one shared manor. |
| "Larger than a pocket dimension" | `ManorBuilder` places the manor: your own saved build if `data/pocketmanor/structure/manor.nbt` is present, otherwise a coded walled 49×33 manor (grand hall + library + kitchen/bedroom), lit so nothing spawns. |
| Designing your own manor | Build it in-game, save it with a Structure Block named `pocketmanor:manor`, drop the `.nbt` into the mod. See **[DESIGN.md](DESIGN.md)**. |
| Built only once | `ManorSavedData` (a `SavedData` on the manor level) records a `built` flag that persists with the world, so the manor is generated a single time and never regenerated. |
| The spell | `PocketManorSpell extends AbstractSpell` (Iron's Spells API), registered through our own `DeferredRegister` hung off `SpellRegistry.SPELL_REGISTRY_KEY`. ENDER school, EPIC rarity, a 3-second channeled (`LONG`) cast. |
| Teleport + return | `ManorManager` toggles you in/out and stores your return point in the player's persisted NBT. Dimension travel uses `ServerPlayer#changeDimension(DimensionTransition)` — the same call the base mod's Recall spell uses. |

### Source layout

```
src/main/java/com/windchaser/pocketmanor/
  PocketManor.java              # @Mod entry point
  registry/PMSpells.java        # DeferredRegister<AbstractSpell> -> registers the spell
  registry/PMDimensions.java    # ResourceKey<Level> for the manor dimension
  spell/PocketManorSpell.java   # the spell definition
  manor/ManorManager.java       # enter/leave logic + return-point storage
  manor/ManorBuilder.java       # generates the manor structure (once)
  manor/ManorSavedData.java     # per-world "already built" flag
src/main/resources/
  data/pocketmanor/dimension/manor.json
  data/pocketmanor/dimension_type/manor.json
  assets/pocketmanor/lang/en_us.json
  assets/pocketmanor/textures/gui/spell_icons/pocket_manor.png
  pack.mcmeta
src/main/templates/META-INF/neoforge.mods.toml   # metadata, filled in at build time
```

---

## Building

This is a standard NeoForge MDK-style Gradle project (Java 21).

> **You must supply the Iron's Spells 'n Spellbooks dependency before it will compile.**
> The build compiles against the ISS API (and the Curios/GeckoLib classes its API references), so
> those jars have to be on the classpath.

1. Open `gradle.properties` and set the CurseForge **file ids** for the exact 1.21.1 / NeoForge
   builds you want to compile against (the numbers come from each file's CurseForge download URL,
   e.g. `.../files/<FILE_ID>`):

   ```properties
   irons_spellbooks_curse_file=<FILE_ID>   # project 855414
   geckolib_curse_file=<FILE_ID>           # project 388172
   curios_curse_file=<FILE_ID>             # project 309927
   ```

   (Prefer a different source? Swap the three `curse.maven:` lines in `build.gradle` for whatever
   maven coordinates you use — e.g. the Modrinth maven `maven.modrinth:...`.)

2. Build:

   ```bash
   ./gradlew build
   ```

   The finished jar lands in `build/libs/`.

3. Run the mod in a dev client (downloads a NeoForge dev environment):

   ```bash
   ./gradlew runClient
   ```

   Drop your ISS + GeckoLib + Curios (+ Player Animator) jars into the generated `run/mods/`
   folder if they aren't already pulled in as runtime dependencies.

### Adjusting the spell

Tune `PocketManorSpell` — `baseManaCost`, `castTime`, `setCooldownSeconds`, `setMinRarity`,
`setMaxLevel`, or the school in `setSchoolResource(...)`. Reshape the building by editing the
constants and helpers in `ManorBuilder`.

---

## Notes & caveats

- **API surface:** written against the Iron's Spells `1.21` branch API (`AbstractSpell#onCast(Level,
  int, LivingEntity, CastSource, MagicData)`, `DefaultConfig`, `SpellRegistry.SPELL_REGISTRY_KEY`,
  `SchoolRegistry.ENDER_RESOURCE`). If you target a different ISS build and an import or signature
  has moved, adjust the affected line — the wiring is intentionally isolated in the `spell/` and
  `registry/` packages.
- **Icon:** a placeholder 16×16 icon is included at
  `assets/pocketmanor/textures/gui/spell_icons/pocket_manor.png`. Replace it with your own art.
- The spell is obtained the same way as any ISS spell (spell scroll / spellbook). It appears in the
  creative inventory under Iron's Spells once ISS is installed.

## License

MIT.
