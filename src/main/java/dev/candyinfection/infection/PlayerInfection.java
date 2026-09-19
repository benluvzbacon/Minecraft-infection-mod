package dev.candyinfection.infection;

import com.mojang.serialization.Codec;
import dev.candyinfection.config.CandyConfig;
import dev.candyinfection.entity.CandyMob;
import dev.candyinfection.init.CandyEffects;
import dev.candyinfection.init.CandyParticles;
import dev.candyinfection.network.InfectionSyncPayload;
import dev.candyinfection.util.CandyLog;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Player infection: a 0-100 percentage stored as a persistent data attachment.
 *
 * <p>Infection is a mechanic, not a death sentence. It creeps up from hostile
 * candy attacks and from standing in dense colonies, decays slowly when you
 * stay clean, and every tier has both a punishment and a visible cue so the
 * player always knows how bad it is. Purification potions, anti-candy syringes
 * and Purifiers all push it back down.
 */
public final class PlayerInfection {
    public static final AttachmentType<Float> LEVEL =
            AttachmentRegistry.createPersistent(dev.candyinfection.CandyInfection.id("infection_level"), Codec.FLOAT);

    private static final Map<UUID, Float> LAST_SYNCED = new HashMap<>();

    public static final int EARLY = 25;
    public static final int SERIOUS = 50;
    public static final int SEVERE = 75;

    private PlayerInfection() {
    }

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getTime() % 20L == 0L) {
                tick(world);
            }
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register(PlayerInfection::onDamaged);
        CandyLog.phase("Player infection system ready (persistent attachment)");
    }

    // ------------------------------------------------------------------ api
    public static float get(PlayerEntity player) {
        Float value = player.getAttached(LEVEL);
        return value == null ? 0.0F : value;
    }

    public static void set(PlayerEntity player, float value) {
        float clamped = Math.max(0.0F, Math.min(100.0F, value));
        player.setAttached(LEVEL, clamped);
        sync(player);
    }

    public static void add(PlayerEntity player, float amount) {
        if (!CandyConfig.get().playerInfectionEnabled || amount == 0.0F) {
            return;
        }
        float before = get(player);
        float after = Math.max(0.0F, Math.min(100.0F, before + amount));
        if (after != before) {
            player.setAttached(LEVEL, after);
            sync(player);
            if (crossedTier(before, after)) {
                onTierChanged(player, after);
            }
        }
    }

    /** Reduces infection, used by cures. */
    public static void cure(PlayerEntity player, float amount) {
        add(player, -amount);
        if (amount > 0.0F && player.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_BREWING_STAND_BREW, SoundCategory.PLAYERS, 0.9F, 1.6F);
            serverWorld.spawnParticles(CandyParticles.PURIFICATION_SPARK, player.getX(), player.getY() + 1.0D, player.getZ(),
                    30, 0.6D, 1.0D, 0.6D, 0.03D);
        }
    }

    /** Multiplier applied to candy damage based on how infected the player is. */
    public static float vulnerability(PlayerEntity player) {
        return 1.0F + get(player) / 200.0F;
    }

    /** True when the player is protected by a Purified effect. */
    public static boolean isProtected(PlayerEntity player) {
        return player.hasStatusEffect(CandyEffects.PURIFIED);
    }

    public static String tierLabel(float level) {
        if (level <= 0.0F) {
            return "HEALTHY";
        }
        if (level < EARLY) {
            return "TRACES";
        }
        if (level < SERIOUS) {
            return "EARLY";
        }
        if (level < SEVERE) {
            return "SERIOUS";
        }
        if (level < 100.0F) {
            return "SEVERE";
        }
        return "TOTAL";
    }

    // ----------------------------------------------------------------- tick
    private static void tick(ServerWorld world) {
        CandyConfig config = CandyConfig.get();
        pruneSyncCache(world);
        InfectionWorldState data = InfectionWorldState.get(world);
        for (ServerPlayerEntity player : world.getPlayers()) {
            float level = get(player);
            BlockPos pos = player.getBlockPos();
            int nearby = InfectionSpread.countInfectedNear(world, pos, 4);
            boolean inColony = data.isColony(pos) || nearby >= 40;

            if (config.playerInfectionEnabled && !isProtected(player)) {
                if (inColony) {
                    add(player, config.infectionPerSecondInColony * (1.0F + nearby / 220.0F));
                } else if (level > 0.0F) {
                    add(player, -config.naturalInfectionDecayPerSecond);
                }
            } else if (level > 0.0F) {
                add(player, -Math.max(0.5F, config.naturalInfectionDecayPerSecond * 4.0F));
            }

            applySymptoms(world, player, get(player), inColony);
            syncIfNeeded(player, get(player), data.getStage(), nearby);
        }
    }

    private static void applySymptoms(ServerWorld world, ServerPlayerEntity player, float level, boolean inColony) {
        if (level <= 0.0F) {
            return;
        }
        int amplifier = level >= SEVERE ? 1 : 0;

        if (level >= EARLY) {
            // Minor visual effects + occasional hunger.
            if (player.age % 60 == 0) {
                world.spawnParticles(CandyParticles.CANDY_DUST, player.getX(), player.getY() + 1.2D, player.getZ(),
                        3, 0.4D, 0.5D, 0.4D, 0.01D);
            }
            if (player.age % 200 == 0) {
                player.addStatusEffect(new StatusEffectInstance(CandyEffects.SUGAR_CRAVING, 200, 0, false, false));
            }
        }
        if (level >= SERIOUS) {
            // Occasional slowness, stronger particle aura, monsters notice you.
            if (player.age % 240 == 0) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 0, false, false));
            }
            attractMonsters(world, player, 24.0D, 1);
        }
        if (level >= SEVERE) {
            // Stronger debuffs and short, involuntary stumbles.
            if (player.age % 180 == 0) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 1, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 80, 0, false, false));
                player.addVelocity((world.random.nextDouble() - 0.5D) * 0.35D, 0.06D, (world.random.nextDouble() - 0.5D) * 0.35D);
                player.velocityModified = true;
                player.sendMessage(Text.translatable("message.candyinfection.stumble"), true);
            }
            attractMonsters(world, player, 40.0D, 2);
        }
        if (level >= 100.0F) {
            // Full infection: severe consequences, but still survivable.
            if (player.age % 40 == 0) {
                player.damage(world.getDamageSources().magic(), 1.5F * CandyConfig.get().infectionDamageMultiplier);
                world.spawnParticles(CandyParticles.INFECTION_SPARK, player.getX(), player.getY() + 1.0D, player.getZ(),
                        12, 0.6D, 0.9D, 0.6D, 0.03D);
            }
            if (player.age % 400 == 0) {
                InfectionRuntime.spawnMonster(world, player.getBlockPos(), false);
                player.sendMessage(Text.translatable("message.candyinfection.fully_infected"), false);
            }
        }
        if (amplifier > 0 && inColony && player.age % 100 == 0) {
            world.spawnParticles(CandyParticles.GUMMY_DROPLET, player.getX(), player.getY() + 0.4D, player.getZ(),
                    4, 0.5D, 0.3D, 0.5D, 0.01D);
        }
    }

    /** Makes nearby candy monsters lock onto a heavily infected player. */
    private static void attractMonsters(ServerWorld world, ServerPlayerEntity player, double radius, int max) {
        if (world.random.nextInt(4) != 0) {
            return;
        }
        Box box = player.getBoundingBox().expand(radius);
        int attracted = 0;
        for (MobEntity mob : world.getEntitiesByClass(MobEntity.class, box, entity -> entity instanceof CandyMob)) {
            if (mob.getTarget() == null && mob.canTarget(player)) {
                mob.setTarget(player);
                attracted++;
                if (attracted >= max) {
                    return;
                }
            }
        }
    }

    private static void onDamaged(LivingEntity entity, DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked) {
        if (!(entity instanceof ServerPlayerEntity player) || damageTaken <= 0.0F) {
            return;
        }
        if (isProtected(player)) {
            return;
        }
        if (source.getAttacker() instanceof CandyMob) {
            add(player, CandyConfig.get().infectionPerAttack);
        }
    }

    private static boolean crossedTier(float before, float after) {
        return tierIndex(before) != tierIndex(after);
    }

    private static int tierIndex(float level) {
        if (level >= 100.0F) {
            return 4;
        }
        if (level >= SEVERE) {
            return 3;
        }
        if (level >= SERIOUS) {
            return 2;
        }
        if (level >= EARLY) {
            return 1;
        }
        return 0;
    }

    private static void onTierChanged(ServerPlayerEntity player, float level) {
        player.sendMessage(Text.translatable("message.candyinfection.tier", tierLabel(level)), true);
        player.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.8F, level >= SERIOUS ? 0.7F : 1.3F);
    }

    // ----------------------------------------------------------------- sync
    public static void sync(ServerPlayerEntity player) {
        float level = get(player);
        int stage = 1;
        int nearby = 0;
        if (player.getWorld() instanceof ServerWorld serverWorld) {
            InfectionWorldState data = InfectionWorldState.get(serverWorld);
            stage = data.getStage();
            nearby = data.nearbyCount(player.getBlockPos());
        }
        LAST_SYNCED.put(player.getUuid(), level);
        ServerPlayNetworking.send(player, new InfectionSyncPayload(level, stage, nearby));
    }

    /**
     * Drops sync-cache entries for players who are no longer online. Called from
     * the world tick so the map cannot grow without bound on a busy server.
     */
    private static void pruneSyncCache(ServerWorld world) {
        if (LAST_SYNCED.size() <= world.getPlayers().size()) {
            return;
        }
        java.util.Set<UUID> online = new java.util.HashSet<>();
        for (ServerPlayerEntity player : world.getPlayers()) {
            online.add(player.getUuid());
        }
        LAST_SYNCED.keySet().retainAll(online);
    }

    private static void syncIfNeeded(ServerPlayerEntity player, float level, int stage, int nearby) {
        Float last = LAST_SYNCED.get(player.getUuid());
        boolean force = last == null || Math.abs(last - level) >= 0.5F;
        if (force || player.age % 40 == 0) {
            LAST_SYNCED.put(player.getUuid(), level);
            ServerPlayNetworking.send(player, new InfectionSyncPayload(level, stage, nearby));
        }
    }
}
