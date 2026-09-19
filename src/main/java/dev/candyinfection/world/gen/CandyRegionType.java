package dev.candyinfection.world.gen;

import net.minecraft.util.math.ChunkPos;

/**
 * The six flavours of "Infected Land".
 *
 * <p>A region is picked deterministically from the chunk coordinates, so a given
 * part of the world always grows the same kind of candy landscape and it looks
 * the same after a restart. Each region has its own palette, decoration and
 * ambient particle mix.
 */
public enum CandyRegionType {
    /** Gummy trees, hanging vines, dripping canopy. */
    GUMMY_FOREST("gummy_forest"),
    /** Barren chocolate crust broken up by melted blobs. */
    CHOCOLATE_WASTELAND("chocolate_wasteland"),
    /** Open fields of sugar crystals and lollipops. */
    SUGAR_CRYSTAL_FIELDS("sugar_crystal_fields"),
    /** Sticky caramel pools and syrup rivers. */
    CARAMEL_SWAMP("caramel_swamp"),
    /** Bright candy plains with hard candy outcrops. */
    CANDY_PLAINS("candy_plains"),
    /** Underground caverns of crystal and hard candy. */
    DEEP_CANDY_CAVERNS("deep_candy_caverns");

    private final String id;

    CandyRegionType(String id) {
        this.id = id;
    }

    public String id() {
        return this.id;
    }

    /** Surface (above y = 0) region for a chunk. */
    public static CandyRegionType forSurfaceChunk(ChunkPos pos) {
        return SURFACE[positiveHash(pos, 0x5DEECE66DL) % SURFACE.length];
    }

    /** Underground region for a chunk. */
    public static CandyRegionType forUndergroundChunk(ChunkPos pos) {
        return UNDERGROUND[positiveHash(pos, 0x27BB2EE6L) % UNDERGROUND.length];
    }

    private static final CandyRegionType[] SURFACE = {GUMMY_FOREST, CHOCOLATE_WASTELAND, SUGAR_CRYSTAL_FIELDS,
            CARAMEL_SWAMP, CANDY_PLAINS};
    private static final CandyRegionType[] UNDERGROUND = {DEEP_CANDY_CAVERNS, SUGAR_CRYSTAL_FIELDS, GUMMY_FOREST};

    private static int positiveHash(ChunkPos pos, long seed) {
        long value = (long) pos.x * 341873128712L + (long) pos.z * 132897987541L + seed;
        value ^= (value >>> 33);
        value *= 0xff51afd7ed558ccdL;
        value ^= (value >>> 29);
        return (int) (Math.abs(value) % Integer.MAX_VALUE);
    }

    public boolean isUnderground() {
        return this == DEEP_CANDY_CAVERNS;
    }
}
