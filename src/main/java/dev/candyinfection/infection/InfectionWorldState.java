package dev.candyinfection.infection;

import dev.candyinfection.util.CandyLog;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtLong;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Per-dimension infection data, saved with the world.
 *
 * <p>Stores the global stage, an approximate infected-block count, a compact
 * per-chunk infection density index (used for "am I in a colony?" checks and
 * the HUD), the set of live infection cores and any active purge zones.
 * Everything here survives a server restart.
 */
public final class InfectionWorldState extends PersistentState {
    public static final String KEY = "candyinfection_data";

    private int stage = InfectionStages.MIN;
    private long activeTicks;
    private int totalInfected;
    private int eventCooldownTicks = 2400;
    private String activeEvent = "";
    private int activeEventTicks;
    private boolean bossSpawned;

    private final Long2IntOpenHashMap chunkCounts = new Long2IntOpenHashMap();
    private final LongList cores = new LongArrayList();
    private final LongList nests = new LongArrayList();
    private final List<PurgeZone> purges = new ArrayList<>();

    /** A region where the infection is dying back after a core was destroyed. */
    public record PurgeZone(BlockPos center, int radius, int ticksLeft) {
    }

    public static InfectionWorldState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
                new PersistentState.Type<>(InfectionWorldState::new, InfectionWorldState::fromNbt, null), KEY);
    }

    public static InfectionWorldState read(ServerWorld world) {
        return world.getPersistentStateManager().get(
                new PersistentState.Type<>(InfectionWorldState::new, InfectionWorldState::fromNbt, null), KEY);
    }

    // ------------------------------------------------------------- stage
    public int getStage() {
        return this.stage;
    }

    public void setStage(int stage) {
        int clamped = Math.max(InfectionStages.MIN, Math.min(InfectionStages.MAX, stage));
        if (clamped != this.stage) {
            this.stage = clamped;
            this.markDirty();
            CandyLog.init("Infection stage changed to " + InfectionStages.of(clamped).label());
        }
    }

    public InfectionStages stageInfo() {
        return InfectionStages.of(this.stage);
    }

    public long getActiveTicks() {
        return this.activeTicks;
    }

    public void addActiveTicks(long ticks) {
        this.activeTicks += ticks;
    }

    // ------------------------------------------------------- infected count
    public int getTotalInfected() {
        return this.totalInfected;
    }

    public void blockInfected() {
        this.totalInfected++;
    }

    public void blockPurified() {
        if (this.totalInfected > 0) {
            this.totalInfected--;
        }
    }

    /** Global infection percentage, capped at 100. */
    public float globalInfectionPercent() {
        // A world "fully infected" for gameplay purposes is ~250k infected blocks.
        float percent = this.totalInfected / 2500.0F;
        percent += (this.stage - 1) * 8.0F;
        percent += this.cores.size() * 3.0F;
        return Math.min(100.0F, percent);
    }

    // --------------------------------------------------------- chunk index
    public void addChunkCount(BlockPos pos, int delta) {
        long key = ChunkPos.toLong(pos);
        int updated = this.chunkCounts.get(key) + delta;
        if (updated <= 0) {
            this.chunkCounts.remove(key);
        } else {
            this.chunkCounts.put(key, updated);
        }
    }

    /** Number of infected blocks recorded in the chunk containing {@code pos}. */
    public int chunkCount(BlockPos pos) {
        return this.chunkCounts.get(ChunkPos.toLong(pos));
    }

    /** Infected blocks in a 3x3 chunk area around {@code pos}. */
    public int nearbyCount(BlockPos pos) {
        ChunkPos center = new ChunkPos(pos);
        int total = 0;
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                total += this.chunkCounts.get(ChunkPos.toLong(center.x + x, center.z + z));
            }
        }
        return total;
    }

    /** True when the area is dense enough to count as Infected Land. */
    public boolean isColony(BlockPos pos) {
        return this.nearbyCount(pos) >= 64;
    }

    // ------------------------------------------------------------- cores
    public void addCore(BlockPos pos) {
        long packed = pos.asLong();
        if (!this.cores.contains(packed)) {
            this.cores.add(packed);
            this.markDirty();
        }
    }

    public void removeCore(BlockPos pos) {
        if (this.cores.rem(pos.asLong())) {
            this.markDirty();
        }
    }

    public int getCoreCount() {
        return this.cores.size();
    }

    /** @return the nearest core position, or {@code null}. */
    public BlockPos nearestCore(BlockPos from) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (long packed : this.cores) {
            BlockPos pos = BlockPos.fromLong(packed);
            double distance = from.getSquaredDistance(pos);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = pos;
            }
        }
        return best;
    }

    // ------------------------------------------------------------ nests
    /**
     * Records an infection nest. Nests are persistent positions the runtime uses
     * as monster spawn points, which is how a colony keeps producing defenders
     * without any block ticking or entity scanning.
     */
    public void addNest(BlockPos pos) {
        long packed = pos.asLong();
        if (this.nests.size() >= 24 || this.nests.contains(packed)) {
            return;
        }
        this.nests.add(packed);
        this.markDirty();
        CandyLog.debug("Infection nest recorded at " + pos.toShortString());
    }

    public boolean removeNest(BlockPos pos) {
        boolean removed = this.nests.rem(pos.asLong());
        if (removed) {
            this.markDirty();
        }
        return removed;
    }

    public int getNestCount() {
        return this.nests.size();
    }

    /** @return the nest closest to {@code from}, or {@code null} if there is none. */
    public BlockPos nearestNest(BlockPos from) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (long packed : this.nests) {
            BlockPos pos = BlockPos.fromLong(packed);
            double distance = from.getSquaredDistance(pos);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = pos;
            }
        }
        return best;
    }

    // ------------------------------------------------------------ purges
    public void addPurge(BlockPos center, int radius, int ticks) {
        this.purges.add(new PurgeZone(center.toImmutable(), radius, ticks));
        this.markDirty();
    }

    public List<PurgeZone> purges() {
        return this.purges;
    }

    public void tickPurges() {
        if (this.purges.isEmpty()) {
            return;
        }
        Iterator<PurgeZone> iterator = this.purges.iterator();
        boolean changed = false;
        List<PurgeZone> updated = new ArrayList<>(this.purges.size());
        while (iterator.hasNext()) {
            PurgeZone zone = iterator.next();
            int left = zone.ticksLeft() - 1;
            iterator.remove();
            if (left > 0) {
                updated.add(new PurgeZone(zone.center(), zone.radius(), left));
            }
            changed = true;
        }
        this.purges.addAll(updated);
        if (changed) {
            this.markDirty();
        }
    }

    // ------------------------------------------------------------ events
    public String getActiveEvent() {
        return this.activeEvent;
    }

    public int getActiveEventTicks() {
        return this.activeEventTicks;
    }

    public void startEvent(String event, int ticks) {
        this.activeEvent = event;
        this.activeEventTicks = ticks;
        this.eventCooldownTicks = ticks + 1200;
        this.markDirty();
        CandyLog.init("Infection event started: " + event);
    }

    public void tickEvent() {
        if (this.activeEventTicks > 0) {
            this.activeEventTicks--;
            if (this.activeEventTicks <= 0) {
                CandyLog.init("Infection event ended: " + this.activeEvent);
                this.activeEvent = "";
            }
        }
        if (this.eventCooldownTicks > 0) {
            this.eventCooldownTicks--;
        }
    }

    public boolean canStartEvent() {
        return this.eventCooldownTicks <= 0 && this.activeEventTicks <= 0;
    }

    public boolean isEventActive(String event) {
        return this.activeEventTicks > 0 && this.activeEvent.equals(event);
    }

    public boolean isBossSpawned() {
        return this.bossSpawned;
    }

    public void setBossSpawned(boolean bossSpawned) {
        this.bossSpawned = bossSpawned;
        this.markDirty();
    }

    // ------------------------------------------------------------- nbt
    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        nbt.putInt("Stage", this.stage);
        nbt.putLong("ActiveTicks", this.activeTicks);
        nbt.putInt("TotalInfected", this.totalInfected);
        nbt.putInt("EventCooldown", this.eventCooldownTicks);
        nbt.putString("ActiveEvent", this.activeEvent);
        nbt.putInt("ActiveEventTicks", this.activeEventTicks);
        nbt.putBoolean("BossSpawned", this.bossSpawned);

        NbtList chunkList = new NbtList();
        for (Long2IntMap.Entry entry : this.chunkCounts.long2IntEntrySet()) {
            NbtCompound chunk = new NbtCompound();
            chunk.putLong("Pos", entry.getLongKey());
            chunk.putInt("Count", entry.getIntValue());
            chunkList.add(chunk);
        }
        nbt.put("Chunks", chunkList);

        NbtList coreList = new NbtList();
        for (long packed : this.cores) {
            coreList.add(NbtLong.of(packed));
        }
        nbt.put("Cores", coreList);

        NbtList purgeList = new NbtList();
        for (PurgeZone zone : this.purges) {
            NbtCompound purge = new NbtCompound();
            purge.put("Center", BlockPos.fromLong(zone.center().asLong()).toNbt());
            purge.putInt("Radius", zone.radius());
            purge.putInt("TicksLeft", zone.ticksLeft());
            purgeList.add(purge);
        }
        nbt.put("Purges", purgeList);

        NbtList nestList = new NbtList();
        for (long packed : this.nests) {
            nestList.add(NbtLong.of(packed));
        }
        nbt.put("Nests", nestList);
        return nbt;
    }

    private static InfectionWorldState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        InfectionWorldState state = new InfectionWorldState();
        state.stage = Math.max(InfectionStages.MIN, Math.min(InfectionStages.MAX, nbt.getInt("Stage")));
        state.activeTicks = nbt.getLong("ActiveTicks");
        state.totalInfected = Math.max(0, nbt.getInt("TotalInfected"));
        state.eventCooldownTicks = nbt.getInt("EventCooldown");
        state.activeEvent = nbt.getString("ActiveEvent");
        state.activeEventTicks = nbt.getInt("ActiveEventTicks");
        state.bossSpawned = nbt.getBoolean("BossSpawned");

        state.chunkCounts.clear();
        NbtList chunkList = nbt.getList("Chunks", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < chunkList.size(); i++) {
            NbtCompound chunk = chunkList.getCompound(i);
            state.chunkCounts.put(chunk.getLong("Pos"), chunk.getInt("Count"));
        }

        state.cores.clear();
        NbtList coreList = nbt.getList("Cores", NbtElement.LONG_TYPE);
        for (int i = 0; i < coreList.size(); i++) {
            state.cores.add(coreList.getLong(i));
        }

        state.purges.clear();
        NbtList purgeList = nbt.getList("Purges", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < purgeList.size(); i++) {
            NbtCompound purge = purgeList.getCompound(i);
            BlockPos center = BlockPos.fromNbt(purge.getCompound("Center"));
            state.purges.add(new PurgeZone(center, purge.getInt("Radius"), purge.getInt("TicksLeft")));
        }
        state.nests.clear();
        NbtList nestList = nbt.getList("Nests", NbtElement.LONG_TYPE);
        for (int i = 0; i < nestList.size(); i++) {
            state.nests.add(nestList.getLong(i));
        }

        CandyLog.debug("Loaded infection state: stage " + state.stage + ", " + state.totalInfected
                + " infected blocks, " + state.cores.size() + " cores, " + state.nests.size() + " nests");
        return state;
    }
}
