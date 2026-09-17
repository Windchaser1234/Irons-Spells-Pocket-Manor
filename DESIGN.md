# Designing your own manor

You don't have to write code to design the manor. **Build it in Minecraft, save it, and the mod
uses your build.** When a structure file is present at
`src/main/resources/data/pocketmanor/structure/manor.nbt`, the mod stamps *that* into the manor
dimension. When it's absent, it falls back to the coded manor in `ManorBuilder`. So designing the
manor is just: build → save → drop the file in → rebuild.

## Step by step

### 1. Build it in a creative world
Make a flat creative world and build your manor. A few things to keep in mind:

- **Include a floor.** The manor dimension is empty void; whatever you don't build isn't there.
  Players who walk off the edge will fall forever, so wall it in or add a railing if you like.
- **Mark where players arrive with a Lodestone.** When your build loads, players are placed on
  **top of the first Lodestone** in it. Put one block wherever the "entrance/landing" should be.
  (No Lodestone? Players land at the horizontal center of the build instead.)
- It's already bright: the dimension has full ambient light, but you can still add lamps/lanterns
  for looks. No hostile mobs will spawn.

### 2. Save it with a Structure Block
1. Give yourself a structure block: `/give @s minecraft:structure_block`
2. Place it, set it to **SAVE** mode.
3. In the **Structure Name** field, type exactly:  `pocketmanor:manor`
4. Set the **size** and **relative position** so the highlighted box covers your whole build.
   - ⚠️ **Size limit: 48 × 48 × 48.** A single structure block can't capture more than that in any
     direction. If your manor is bigger, keep it within 48 per axis for now — or tell me and I'll
     add multi-piece support so you can save it in sections.
5. Toggle "Include entities" off (recommended), then click **SAVE**.

The game writes the file to:

```
.minecraft/saves/<your world>/generated/pocketmanor/structures/manor.nbt
```

### 3. Put it in the mod
Copy that `manor.nbt` into the mod, into this exact path (note **structure**, singular):

```
src/main/resources/data/pocketmanor/structure/manor.nbt
```

### 4. Rebuild
```bash
./gradlew build
```

Your build now loads whenever the manor is first entered in a world.

## Where the build is placed
The structure's `(0,0,0)` corner (the north-west-bottom corner of the box you saved) is placed at
world position **`(0, 64, 0)`** in the manor dimension, extending **+X (east)**, **+Y (up)**, and
**+Z (south)** from there. The Lodestone you placed defines the exact spot players land on, so the
placement corner mostly doesn't matter — build wherever is convenient inside the structure box.

## Re-testing after you change the build
The manor is generated **once per world** and then remembered (that's what makes it a single shared
manor). So if you edit `manor.nbt` and want to see the change, do one of:

- Test in a **brand-new world**, or
- Stop the game and delete the manor's saved state from the world folder:
  - `saves/<world>/dimensions/pocketmanor/manor/`  (the generated chunks)
  - `saves/<world>/data/pocketmanor_manor.dat`      (the "already built" flag)

  Next time someone casts the spell, it rebuilds from your latest `manor.nbt`.

(If you'd rather have an in-game `/pocketmanor rebuild` command for iterating, say the word and I'll
add one.)

## Prefer other tools?
- **Litematica / WorldEdit / Amulet / MCEdit** can all export/convert to vanilla `.nbt` structures —
  save or convert to `.nbt`, name it `manor.nbt`, and drop it in the same place.
- If you specifically want to load WorldEdit `.schem` files directly (no conversion), that needs an
  extra schematic-parsing dependency — ask and I'll wire it in.
