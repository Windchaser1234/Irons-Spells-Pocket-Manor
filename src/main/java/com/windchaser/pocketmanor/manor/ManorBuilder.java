package com.windchaser.pocketmanor.manor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;

/**
 * Generates the shared manor once, into the empty (void) manor dimension.
 *
 * <p>The build is centered on the origin and floats on its own foundation at {@link #FLOOR_Y}.
 * It is deliberately larger and more furnished than a plain pocket-dimension room: a walled manor
 * with a grand central hall, a library wing, and a kitchen/bedroom wing, all lit so nothing spawns.
 *
 * <p>All placement uses flag {@code 2} (send to clients, skip neighbor updates) which is the right
 * choice for bulk world edits - it keeps generation cheap and avoids cascading block updates.
 */
public final class ManorBuilder {
    private static final int FLOOR_Y = 64;
    private static final int WALL_TOP = FLOOR_Y + 8;      // walls occupy FLOOR_Y+1 .. WALL_TOP
    private static final int CEIL_Y = WALL_TOP + 1;       // solid ceiling

    // Interior half-extents (walls sit on these lines).
    private static final int HX = 24;
    private static final int HZ = 16;
    // Outdoor terrace half-extents.
    private static final int TX = 28;
    private static final int TZ = 20;

    private ManorBuilder() {
    }

    /** Where a player materializes when entering the manor. */
    public static Vec3 entrancePos() {
        return new Vec3(0.5, FLOOR_Y + 1, 12.5);
    }

    public static void ensureBuilt(ServerLevel manor) {
        ManorSavedData data = ManorSavedData.get(manor);
        if (data.built) {
            return;
        }
        build(manor);
        data.built = true;
        data.setDirty();
    }

    private static void build(ServerLevel l) {
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
