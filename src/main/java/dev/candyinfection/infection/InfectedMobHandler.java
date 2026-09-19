package dev.candyinfection.infection;

import com.mojang.serialization.Codec;
import dev.candyinfection.CandyInfection;
import dev.candyinfection.config.CandyConfig;
import dev.candyinfection.entity.CandyMob;
import dev.candyinfection.init.CandyItems;
import dev.candyinfection.init.CandyParticles;
import dev.candyinfection.util.CandyLog;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Turns ordinary Minecraft mobs into infected variants.
 *
 * <p>An infected mob is not a recoloured mob: it gets real attribute modifiers,
 * an aggressive behaviour loop (vanilla pathfinding plus vanilla melee attacks),
 * a permanent candy particle aura, it seeds candy growth wherever it walks, it
 * spreads the infection when it is hurt, and it drops candy materials when it
 * dies. Which mobs are allowed to turn - and how long they have to stand on
 * infected ground first - is configurable.
 */
public final class InfectedMobHandler {
    public static final AttachmentType<Boolean> INFECTED =
            AttachmentRegistry.createPersistent(CandyInfection.id("mob_infected"), Codec.BOOL);
    public static final AttachmentType<Integer> EXPOSURE =
            AttachmentRegistry.createPersistent(CandyInfection.id("mob_infection_exposure"), Codec.INT);

    private static final Identifier SPEED_ID = CandyInfection.id("infected_speed");
    private static final Identifier HEALTH_ID = CandyInfection.id("infected_health");
    private static final Identifier DAMAGE_ID = CandyInfection.id("infected_damage");
    private static final Identifier ARMOR_ID = CandyInfection.id("infected_armor");
    private static final Identifier KNOCKBACK_ID = CandyInfection.id("infected_knockback");

    private InfectedMobHandler() {
    }

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getTime() % 40L == 0L) {
                scan(world);
            }
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register(InfectedMobHandler::onDamaged);
        ServerLivingEntityEvents.AFTER_DEATH.register(InfectedMobHandler::onDeath);
        CandyLog.phase("Vanilla mob infection handler ready (configurable)");
    }

    public static boolean isInfected(Entity entity) {
        Boolean infected = entity.getAttached(INFECTED);
        return infected != null && infected;
    }

    /** Immediately converts a mob into its infected variant. */
    public static void infect(MobEntity mob) {
        if (isInfected(mob) || !(mob.getWorld() instanceof ServerWorld world)) {
            return;
        }
        mob.setAttached(INFECTED, Boolean.TRUE);
        mob.setAttached(EXPOSURE, 0);
        mob.setPersistent();

        applyModifiers(mob, EntityAttributes.GENERIC_MOVEMENT_SPEED, SPEED_ID, 0.45D,
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        applyModifiers(mob, EntityAttributes.GENERIC_MAX_HEALTH, HEALTH_ID, 8.0D,
                EntityAttributeModifier.Operation.ADD_VALUE);
        applyModifiers(mob, EntityAttributes.GENERIC_ATTACK_DAMAGE, DAMAGE_ID, 3.0D,
                EntityAttributeModifier.Operation.ADD_VALUE);
        applyModifiers(mob, EntityAttributes.GENERIC_ARMOR, ARMOR_ID, 4.0D,
                EntityAttributeModifier.Operation.ADD_VALUE);
        applyModifiers(mob, EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, KNOCKBACK_ID, 0.5D,
                EntityAttributeModifier.Operation.ADD_VALUE);
        mob.heal(8.0F);

        String name = infectedName(mob.getType());
        mob.setCustomName(Text.translatable("entity.candyinfection.infected", Text.translatable(mob.getType().getTranslationKey())));
        mob.setCustomNameVisible(false);
        world.playSound(null, mob.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.NEUTRAL, 1.2F, 0.7F);
        world.spawnParticles(CandyParticles.INFECTION_SPARK, mob.getX(), mob.getY() + 0.8D, mob.getZ(), 24, 0.5D, 0.7D, 0.5D, 0.03D);
        CandyLog.debug(name + " infected at " + mob.getBlockPos().toShortString());
    }

    private static void applyModifiers(MobEntity mob,
                                       net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.attribute.EntityAttribute> attribute,
                                       Identifier id, double amount, EntityAttributeModifier.Operation operation) {
        EntityAttributeInstance instance = mob.getAttributeInstance(attribute);
        if (instance == null || instance.getModifier(id) != null) {
            return;
        }
        instance.addPersistentModifier(new EntityAttributeModifier(id, amount, operation));
    }

    /** The display name shown when a mob turns, e.g. "Candy Cow". */
    public static String infectedName(EntityType<?> type) {
        Identifier id = Registries.ENTITY_TYPE.getId(type);
        return "Candy " + id.getPath().replace('_', ' ');
    }

    private static void scan(ServerWorld world) {
        CandyConfig config = CandyConfig.get();
        if (!config.infectVanillaMobs) {
            return;
        }
        for (Entity entity : world.iterateEntities()) {
            if (!(entity instanceof MobEntity mob) || mob instanceof CandyMob || mob instanceof PlayerEntity) {
                continue;
            }
            if (isInfected(mob)) {
                infectedBehaviour(world, mob);
            } else {
                accumulateExposure(world, mob, config);
            }
        }
    }

    private static void accumulateExposure(ServerWorld world, MobEntity mob, CandyConfig config) {
        Identifier id = Registries.ENTITY_TYPE.getId(mob.getType());
        if (!config.canInfect(id.toString())) {
            return;
        }
        BlockPos below = mob.getBlockPos().down();
        if (!InfectionConversions.isInfected(world.getBlockState(below))) {
            Integer exposure = mob.getAttached(EXPOSURE);
            if (exposure != null && exposure > 0) {
                mob.setAttached(EXPOSURE, Math.max(0, exposure - 20));
            }
            return;
        }
        Integer exposure = mob.getAttached(EXPOSURE);
        int total = (exposure == null ? 0 : exposure) + 40;
        mob.setAttached(EXPOSURE, total);
        if (total >= config.vanillaMobInfectionTicks
                && world.random.nextFloat() < config.vanillaMobInfectionChance) {
            infect(mob);
        }
    }

    /** Behaviour loop for an already infected mob. */
    private static void infectedBehaviour(ServerWorld world, MobEntity mob) {
        // Candy particle aura - the visible "this thing is wrong" cue.
        if (mob.age % 20 == 0) {
            world.spawnParticles(CandyParticles.CANDY_DUST, mob.getX(), mob.getY() + mob.getHeight() * 0.6D, mob.getZ(),
                    2, 0.35D, 0.35D, 0.35D, 0.01D);
        }
        // Leave candy growth behind.
        if (mob.age % 100 == 0 && world.random.nextInt(3) == 0) {
            BlockPos below = mob.getBlockPos().down();
            BlockState ground = world.getBlockState(below);
            if (ground.isOf(net.minecraft.block.Blocks.GRASS_BLOCK) || ground.isOf(net.minecraft.block.Blocks.DIRT)) {
                InfectionSpread.convert(world, below, ground, CandyBlocksRef.INFECTED_GRASS.getDefaultState());
            } else if (InfectionConversions.isInfected(ground) && world.isAir(mob.getBlockPos()) && world.random.nextInt(2) == 0) {
                BlockState growth = InfectionConversions.randomVegetation(world.random, InfectionWorldState.get(world).getStage());
                if (growth != null && growth.canPlaceAt(world, mob.getBlockPos())) {
                    InfectionSpread.convert(world, mob.getBlockPos(), world.getBlockState(mob.getBlockPos()), growth);
                }
            }
        }
        // Hunt the nearest player using vanilla pathfinding + vanilla attacks.
        if (mob.age % 10 != 0) {
            return;
        }
        PlayerEntity target = world.getClosestPlayer(mob, 20.0D);
        if (target == null || !mob.canTarget(target) || target.isCreative() || target.isSpectator()) {
            return;
        }
        mob.setTarget(target);
        double speed = 1.15D;
        if (mob instanceof PathAwareEntity pathAware) {
            pathAware.getNavigation().startMovingTo(target, speed);
        }
        if (mob.squaredDistanceTo(target) <= 6.25D) {
            mob.tryAttack(target);
            if (world.random.nextInt(4) == 0) {
                target.addStatusEffect(new StatusEffectInstance(CandyEffectsRef.STICKY, 100, 0, false, true));
            }
        }
    }

    private static void onDamaged(LivingEntity entity, DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked) {
        if (damageTaken <= 0.0F || !(entity.getWorld() instanceof ServerWorld world)) {
            return;
        }
        if (isInfected(entity)) {
            // Hurt infected mobs spray infection around the wound.
            BlockPos pos = entity.getBlockPos();
            InfectionSpread.onInfectedBlockTick(world, pos.down(), world.getBlockState(pos.down()), 3.0F);
            world.spawnParticles(CandyParticles.GUMMY_DROPLET, entity.getX(), entity.getY() + 0.6D, entity.getZ(),
                    8, 0.4D, 0.5D, 0.4D, 0.02D);
            if (source.getAttacker() instanceof PlayerEntity player && !PlayerInfection.isProtected(player)
                    && world.random.nextFloat() < 0.25F) {
                PlayerInfection.add(player, 2.0F);
            }
        }
    }

    private static void onDeath(LivingEntity entity, DamageSource source) {
        if (!(entity.getWorld() instanceof ServerWorld world) || !isInfected(entity)) {
            return;
        }
        BlockPos pos = entity.getBlockPos();
        world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.NEUTRAL, 1.0F, 1.2F);
        world.spawnParticles(CandyParticles.CHOCOLATE_FRAGMENT, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                24, 0.5D, 0.6D, 0.5D, 0.05D);

        // Candy loot.
        drop(world, pos, new ItemStack(CandyItems.GUMMY_RESIN, 1 + world.random.nextInt(3)));
        if (world.random.nextBoolean()) {
            drop(world, pos, new ItemStack(CandyItems.SUGAR_SHARD, 1 + world.random.nextInt(2)));
        }
        if (world.random.nextInt(6) == 0) {
            drop(world, pos, new ItemStack(CandyItems.INFECTION_CRYSTAL));
        }

        // The corpse seeds the ground.
        int converted = InfectionConversions.infectArea(world, pos, 3.0D, 0.8F,
                InfectionWorldState.get(world).getStage(), world.random);
        if (converted > 0) {
            CandyLog.debug("Infected mob death seeded " + converted + " blocks");
        }
    }

    private static void drop(ServerWorld world, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        world.spawnEntity(new ItemEntity(world, pos.getX() + 0.5D, pos.getY() + 0.3D, pos.getZ() + 0.5D, stack));
    }

    /** Small indirection so this class does not force block classes to load early. */
    private static final class CandyBlocksRef {
        private static final Block INFECTED_GRASS = dev.candyinfection.init.CandyBlocks.INFECTED_GRASS_BLOCK;
    }

    /** Same idea for effects. */
    private static final class CandyEffectsRef {
        private static final net.minecraft.entity.effect.StatusEffect STICKY = dev.candyinfection.init.CandyEffects.STICKY;
    }
}
