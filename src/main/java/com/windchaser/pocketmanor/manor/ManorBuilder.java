package com.windchaser.pocketmanor.manor;

import com.windchaser.pocketmanor.PocketManor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Puts the shared manor into the empty (void) manor dimension, exactly once per world.
 *
 * <p>Two sources, in priority order:
 * <ol>
 *   <li><b>A designed structure</b> - if the datapack contains a structure template at
 *       {@code data/pocketmanor/structure/manor.nbt} (i.e. a build you saved in-game with a
 *       Structure Block), it is stamped into the world at {@link #STRUCTURE_ORIGIN}. See
 *       {@code DESIGN.md}. This is the intended way to design the manor.</li>
 *   <li><b>A coded fallback</b> - if no such structure ships in the jar, the procedural manor
 *       below is generated instead, so the mod always produces a usable manor out of the box.</li>
 * </ol>
 *
 * <p>All procedural placement uses flag {@code 2} (send to clients, skip neighbor updates) which is
 * the right choice for bulk world edits - cheap, and avoids cascading block updates.
 */
public final class ManorBuilder {
    /** Datapack id of the player-designed structure (optional). */
    public static final ResourceLocation STRUCTURE_ID =
            ResourceLocation.fromNamespaceAndPath(PocketManor.MOD_ID, "manor");

    /** The world position the structure's (0,0,0) corner is placed at. */
    private static final BlockPos STRUCTURE_ORIGIN = new BlockPos(0, 64, 0);

    private static final int FLOOR_Y = 64;
    private static final int WALL_TOP = FLOOR_Y + 8;      // walls occupy FLOOR_Y+1 .. WALL_TOP
    private static final int CEIL_Y = WALL_TOP + 1;       // solid ceiling

    // Interior half-extents (walls sit on these lines).
    private static final int HX = 24;
    private static final int HZ = 16;
    // Outdoor terrace half-extents.
    private static final int TX = 28;
    private static final int TZ = 20;

    /** Landing spot used by the coded fallback manor. */
    private static final Vec3 FALLBACK_ENTRANCE = new Vec3(0.5, FLOOR_Y + 1, 12.5);

    private ManorBuilder() {
    }

    /** Where a player materializes when entering the manor (resolved at build time). */
    public static Vec3 entrancePos(ServerLevel manor) {
        ManorSavedData data = ManorSavedData.get(manor);
        if (data.hasEntrance) {
            return new Vec3(data.entranceX, data.entranceY, data.entranceZ);
        }
        return FALLBACK_ENTRANCE;
    }

    public static void ensureBuilt(ServerLevel manor) {
        ManorSavedData data = ManorSavedData.get(manor);
        if (data.built) {
            return;
        }

        Vec3 entrance = placeDesignedStructure(manor);
        if (entrance == null) {
            buildFallback(manor);
            entrance = FALLBACK_ENTRANCE;
        }

        data.entranceX = entrance.x;
        data.entranceY = entrance.y;
        data.entranceZ = entrance.z;
        data.hasEntrance = true;
        data.built = true;
        data.setDirty();
    }

    /**
     * Places the player-designed structure if one is shipped in the datapack.
     *
     * @return the arrival position (one block above the first Lodestone in the build, or the
     *         build's horizontal center if there is none), or {@code null} if no structure exists.
     */
    private static Vec3 placeDesignedStructure(ServerLevel manor) {
        StructureTemplateManager manager = manor.getStructureManager();
        Optional<StructureTemplate> maybeTemplate = manager.get(STRUCTURE_ID);
        if (maybeTemplate.isEmpty()) {
            return null;
        }

        StructureTemplate template = maybeTemplate.get();
        StructurePlaceSettings settings = new StructurePlaceSettings();
        template.placeInWorld(manor, STRUCTURE_ORIGIN, STRUCTURE_ORIGIN, settings,
                manor.getRandom(), Block.UPDATE_CLIENTS);

        Vec3i size = template.getSize();

        // Prefer an explicit arrival marker: the first Lodestone found in the build.
        for (int y = 0; y < size.getY(); y++) {
            for (int x = 0; x < size.getX(); x++) {
                for (int z = 0; z < size.getZ(); z++) {
                    BlockPos pos = STRUCTURE_ORIGIN.offset(x, y, z);
                    if (manor.getBlockState(pos).is(Blocks.LODESTONE)) {
                        return new Vec3(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
                    }
                }
            }
        }

        // No marker: drop the player in at the horizontal center, just above the base layer.
        return new Vec3(
                STRUCTURE_ORIGIN.getX() + size.getX() / 2.0,
                STRUCTURE_ORIGIN.getY() + 1,
                STRUCTURE_ORIGIN.getZ() + size.getZ() / 2.0);
    }

    private static void buildFallback(ServerLevel l) {
        BlockState stoneBricks = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState chiseled = Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
        BlockState smoothStone = Blocks.SMOOTH_STONE.defaultBlockState();
        BlockState planks = Blocks.SPRUCE_PLANKS.defaultBlockState();

        // 1) Foundation + floor (covers the whole terrace footprint).
        fill(l, -TX, FLOOR_Y - 1, -TZ, TX, FLOOR_Y - 1, TZ, Blocks.STONE.defaultBlockState());
        fill(l, -TX, FLOOR_Y, -TZ, TX, FLOOR_Y, TZ, stoneBricks);
        // Interior floor is warm spruce so the hall reads as "indoors".
        fill(l, -HX + 1, FLOOR_Y, -HZ + 1, HX - 1, FLOOR_Y, HZ - 1, planks);

        // 2) Terrace safety lip so players don't immediately walk off into the void.
        outline(l, -TX, FLOOR_Y + 1, -TZ, TX, FLOOR_Y + 1, TZ, Blocks.STONE_BRICK_WALL.defaultBlockState());

        // 3) Outer walls (hollow box), then carve a doorway on the south wall.
        walls(l, -HX, HZ, stoneBricks);
        fill(l, -1, FLOOR_Y + 1, HZ, 1, FLOOR_Y + 3, HZ, Blocks.AIR.defaultBlockState()); // entrance gap

        // 4) Corner pillars for a bit of grandeur.
        pillar(l, -HX, -HZ, chiseled);
        pillar(l, -HX, HZ, chiseled);
        pillar(l, HX, -HZ, chiseled);
        pillar(l, HX, HZ, chiseled);

        // 5) Ceiling + lantern grid.
        fill(l, -HX, CEIL_Y, -HZ, HX, CEIL_Y, HZ, smoothStone);
        for (int x = -HX + 4; x <= HX - 4; x += 6) {
            for (int z = -HZ + 4; z <= HZ - 4; z += 6) {
                set(l, x, CEIL_Y, z, Blocks.SEA_LANTERN.defaultBlockState());
            }
        }

        // 6) Windows: punch glass panes into the long walls at eye height.
        BlockState glass = Blocks.GLASS_PANE.defaultBlockState();
        for (int x = -HX + 4; x <= HX - 4; x += 5) {
            set(l, x, FLOOR_Y + 3, -HZ, glass);
            set(l, x, FLOOR_Y + 3, HZ, glass);
        }
        for (int z = -HZ + 4; z <= HZ - 4; z += 5) {
            set(l, -HX, FLOOR_Y + 3, z, glass);
            set(l, HX, FLOOR_Y + 3, z, glass);
        }

        // 7) Interior partition walls (with doorways) that split the manor into three wings.
        partitionAlongX(l, -8, stoneBricks);  // library wing to the west
        partitionAlongX(l, 10, stoneBricks);  // kitchen/bedroom wing to the east

        // 8) Furnish.
        buildLibrary(l);
        buildHall(l);
        buildKitchen(l);
        buildBedroom(l);
        buildEntrance(l);
    }

    // ---- Wing furnishings --------------------------------------------------------------------

    private static void buildLibrary(ServerLevel l) {
        BlockState shelf = Blocks.BOOKSHELF.defaultBlockState();
        // Line the far west wall with bookshelves, two high.
        for (int z = -HZ + 2; z <= HZ - 2; z++) {
            set(l, -HX + 1, FLOOR_Y + 1, z, shelf);
            set(l, -HX + 1, FLOOR_Y + 2, z, shelf);
        }
        // A little enchanting nook with a ring of bookshelves around the table.
        int ex = -16, ez = 0;
        set(l, ex, FLOOR_Y + 1, ez, Blocks.ENCHANTING_TABLE.defaultBlockState());
        for (int dz = -2; dz <= 2; dz++) {
            set(l, ex - 2, FLOOR_Y + 1, ez + dz, shelf);
            set(l, ex - 2, FLOOR_Y + 2, ez + dz, shelf);
            set(l, ex + 2, FLOOR_Y + 1, ez + dz, shelf);
            set(l, ex + 2, FLOOR_Y + 2, ez + dz, shelf);
        }
        for (int dx = -1; dx <= 1; dx++) {
            set(l, ex + dx, FLOOR_Y + 1, ez - 2, shelf);
            set(l, ex + dx, FLOOR_Y + 1, ez + 2, shelf);
        }
        set(l, ex, FLOOR_Y + 1, ez - 3, Blocks.LECTERN.defaultBlockState());
        set(l, ex, FLOOR_Y + 1, ez + 3, Blocks.CHEST.defaultBlockState());
        // Reading-room rug + lights.
        fill(l, -HX + 3, FLOOR_Y + 1, -3, -12, FLOOR_Y + 1, 3, Blocks.RED_CARPET.defaultBlockState());
        set(l, -HX + 2, FLOOR_Y + 1, -HZ + 2, Blocks.LANTERN.defaultBlockState());
        set(l, -HX + 2, FLOOR_Y + 1, HZ - 2, Blocks.LANTERN.defaultBlockState());
    }

    private static void buildHall(ServerLevel l) {
        // Carpet runner straight down the middle, passing through the doorways at z = 0.
        for (int x = -HX + 2; x <= HX - 2; x++) {
            if (!isPartitionColumn(x)) {
                set(l, x, FLOOR_Y + 1, 0, Blocks.CYAN_CARPET.defaultBlockState());
            }
        }
        // A small welcoming chandelier of sea lanterns just under the ceiling, center hall.
        set(l, 0, CEIL_Y - 1, 0, Blocks.SEA_LANTERN.defaultBlockState());
        set(l, 1, CEIL_Y - 1, 0, Blocks.SEA_LANTERN.defaultBlockState());
        set(l, 0, CEIL_Y - 1, 1, Blocks.SEA_LANTERN.defaultBlockState());
        // Decorative flower pots on the hall floor.
        set(l, -6, FLOOR_Y + 1, -6, Blocks.POTTED_AZURE_BLUET.defaultBlockState());
        set(l, -6, FLOOR_Y + 1, 6, Blocks.POTTED_POPPY.defaultBlockState());
        set(l, 8, FLOOR_Y + 1, -6, Blocks.POTTED_BLUE_ORCHID.defaultBlockState());
        set(l, 8, FLOOR_Y + 1, 6, Blocks.POTTED_OXEYE_DAISY.defaultBlockState());
    }

    private static void buildKitchen(ServerLevel l) {
        int z = HZ - 2;
        set(l, 13, FLOOR_Y + 1, z, Blocks.CRAFTING_TABLE.defaultBlockState());
        set(l, 14, FLOOR_Y + 1, z, Blocks.FURNACE.defaultBlockState());
        set(l, 15, FLOOR_Y + 1, z, Blocks.SMOKER.defaultBlockState());
        set(l, 16, FLOOR_Y + 1, z, Blocks.BLAST_FURNACE.defaultBlockState());
        set(l, 17, FLOOR_Y + 1, z, Blocks.CARTOGRAPHY_TABLE.defaultBlockState());
        set(l, 18, FLOOR_Y + 1, z, Blocks.BREWING_STAND.defaultBlockState());
        set(l, 19, FLOOR_Y + 1, z, Blocks.CAULDRON.defaultBlockState());
        set(l, 20, FLOOR_Y + 1, z, Blocks.BARREL.defaultBlockState());
        // Storage row.
        set(l, 14, FLOOR_Y + 1, z - 1, Blocks.CHEST.defaultBlockState());
        set(l, 15, FLOOR_Y + 1, z - 1, Blocks.CHEST.defaultBlockState());
        fill(l, 13, FLOOR_Y + 1, 2, HX - 2, FLOOR_Y + 1, HZ - 4, Blocks.LIGHT_GRAY_CARPET.defaultBlockState());
    }

    private static void buildBedroom(ServerLevel l) {
        // Bedroom occupies the north end of the east wing.
        Direction facing = Direction.WEST;
        BlockPos foot = new BlockPos(20, FLOOR_Y + 1, -12);
        BlockPos head = foot.relative(facing);
        set(l, foot, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, facing).setValue(BedBlock.PART, BedPart.FOOT));
        set(l, head, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, facing).setValue(BedBlock.PART, BedPart.HEAD));
        set(l, 20, FLOOR_Y + 1, -10, Blocks.CHEST.defaultBlockState());
        set(l, 13, FLOOR_Y + 1, -12, Blocks.BOOKSHELF.defaultBlockState());
        set(l, 13, FLOOR_Y + 1, -13, Blocks.LANTERN.defaultBlockState());
        fill(l, 13, FLOOR_Y + 1, -HZ + 2, HX - 2, FLOOR_Y + 1, -6, Blocks.BLUE_CARPET.defaultBlockState());
    }

    private static void buildEntrance(ServerLevel l) {
        // A lodestone "arrival pad" and lanterns to frame the doorway.
        set(l, 0, FLOOR_Y, 13, Blocks.LODESTONE.defaultBlockState());
        set(l, -1, FLOOR_Y + 1, 14, Blocks.LANTERN.defaultBlockState());
        set(l, 1, FLOOR_Y + 1, 14, Blocks.LANTERN.defaultBlockState());
        set(l, -2, FLOOR_Y + 1, HZ - 1, Blocks.POTTED_OAK_SAPLING.defaultBlockState());
        set(l, 2, FLOOR_Y + 1, HZ - 1, Blocks.POTTED_OAK_SAPLING.defaultBlockState());
    }

    // ---- Structural helpers ------------------------------------------------------------------

    private static void walls(ServerLevel l, int minXZ, int maxXZ, BlockState state) {
        // minXZ/maxXZ passed as (-HX, HZ) but we use the symmetric extents directly.
        for (int y = FLOOR_Y + 1; y <= WALL_TOP; y++) {
            for (int x = -HX; x <= HX; x++) {
                set(l, x, y, -HZ, state);
                set(l, x, y, HZ, state);
            }
            for (int z = -HZ; z <= HZ; z++) {
                set(l, -HX, y, z, state);
                set(l, HX, y, z, state);
            }
        }
    }

    private static void pillar(ServerLevel l, int x, int z, BlockState state) {
        for (int y = FLOOR_Y + 1; y <= CEIL_Y; y++) {
            set(l, x, y, z, state);
        }
    }

    private static void partitionAlongX(ServerLevel l, int x, BlockState state) {
        for (int y = FLOOR_Y + 1; y <= WALL_TOP; y++) {
            for (int z = -HZ + 1; z <= HZ - 1; z++) {
                set(l, x, y, z, state);
            }
        }
        // Doorway gap at z in [-1,1], 3 tall.
        fill(l, x, FLOOR_Y + 1, -1, x, FLOOR_Y + 3, 1, Blocks.AIR.defaultBlockState());
    }

    private static boolean isPartitionColumn(int x) {
        return x == -8 || x == 10;
    }

    // ---- Low-level fills ---------------------------------------------------------------------

    private static void outline(ServerLevel l, int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            set(l, x, y1, Math.min(z1, z2), s);
            set(l, x, y1, Math.max(z1, z2), s);
        }
        for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
            set(l, Math.min(x1, x2), y1, z, s);
            set(l, Math.max(x1, x2), y1, z, s);
        }
    }

    private static void fill(ServerLevel l, int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    set(l, x, y, z, s);
                }
            }
        }
    }

    private static void set(ServerLevel l, int x, int y, int z, BlockState s) {
        l.setBlock(new BlockPos(x, y, z), s, 2);
    }

    private static void set(ServerLevel l, BlockPos pos, BlockState s) {
        l.setBlock(pos, s, 2);
    }
}
