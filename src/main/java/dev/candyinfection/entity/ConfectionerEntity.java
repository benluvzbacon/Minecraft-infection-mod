package dev.candyinfection.entity;

import dev.candyinfection.init.CandyEntities;
import dev.candyinfection.init.CandyItems;
import dev.candyinfection.init.CandyParticles;
import dev.candyinfection.infection.InfectionConversions;
import dev.candyinfection.infection.InfectionRuntime;
import dev.candyinfection.infection.InfectionWorldState;
import dev.candyinfection.infection.PlayerInfection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

/**
 * The Confectioner: the boss of the mod.
 *
 * <p>Five phases, each with its own behaviour rather than just more health:
 * <ol>
 *   <li><b>Melee</b> - heavy swings and knockback.</li>
 *   <li><b>Summoning</b> - calls candy monsters into the arena.</li>
 *   <li><b>Infection zones</b> - drops caramel traps and converts ground.</li>
 *   <li><b>Overgrowth</b> - rapidly spreads candy blocks across the arena.</li>
 *   <li><b>Enraged</b> - all attacks at once, faster and harder.</li>
 * </ol>
 * Attacks: candy projectile barrage, gummy tendrils, caramel traps, chocolate
 * explosions, sugar crystal spikes, monster summoning and an infection wave.
 */
public class ConfectionerEntity extends CandyHostileEntity {
    private final ServerBossBar bossBar = new ServerBossBar(Text.translatable("entity.candyinfection.the_confectioner"),
            BossBar.Color.PINK, BossBar.Style.PROGRESS);

    private int barrageCooldown = 120;
    private int summonCooldown = 320;
    private int trapCooldown = 240;
    private int spikeCooldown = 200;
    private int waveCooldown = 500;
    private int explosionCooldown = 420;

    public ConfectionerEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.bossBar.setDarkenSky(true);
        this.setPersistent();
    }

    public static DefaultAttributeContainer.Builder createConfectionerAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 420.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.32D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 18.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 16.0D)
                .add(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, 6.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0D);
    }

    /** 1..5, derived from remaining health. */
    public int getPhase() {
        float ratio = this.getHealth() / this.getMaxHealth();
        if (ratio > 0.8F) {
            return 1;
        }
        if (ratio > 0.6F) {
            return 2;
        }
        if (ratio > 0.4F) {
            return 3;
        }
        if (ratio > 0.2F) {
            return 4;
        }
        return 5;
    }

    @Override
    protected void initGoals() {
        super.initGoals();
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        if (this.getWorld().isClient) {
            return;
        }
        ServerWorld world = (ServerWorld) this.getWorld();
        this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        int phase = this.getPhase();
        float speed = phase >= 5 ? 1.35F : 1.0F;
        if (--this.barrageCooldown <= 0) {
            this.barrageCooldown = (int) (160 / speed);
            this.barrage(world, target, phase);
        }
        if (phase >= 2 && --this.summonCooldown <= 0) {
            this.summonCooldown = (int) (420 / speed);
            this.summon(world, phase);
        }
        if (phase >= 3 && --this.trapCooldown <= 0) {
            this.trapCooldown = (int) (300 / speed);
            this.caramelTraps(world, target);
        }
        if (--this.spikeCooldown <= 0) {
            this.spikeCooldown = (int) (260 / speed);
            this.sugarSpikes(world);
        }
        if (phase >= 4 && --this.explosionCooldown <= 0) {
            this.explosionCooldown = (int) (520 / speed);
            this.chocolateExplosion(world);
        }
        if (phase >= 3 && --this.waveCooldown <= 0) {
            this.waveCooldown = (int) (600 / speed);
            this.infectionWave(world);
        }
    }

    /** Candy projectile barrage: a fan of sticky projectiles. */
    private void barrage(ServerWorld world, LivingEntity target, int phase) {
        int count = 3 + phase;
        for (int i = 0; i < count; i++) {
            var projectile = CandyEntities.CANDY_PROJECTILE.create(world);
            if (projectile == null) {
                continue;
            }
            projectile.setOwner(this);
            projectile.refreshPositionAndAngles(this.getX(), this.getEyeY(), this.getZ(), this.getYaw(), this.getPitch());
            float spread = (i - (count - 1) / 2.0F) * 6.0F;
            projectile.setVelocity(target.getX() - this.getX(), target.getEyeY() - projectile.getY(),
                    target.getZ() - this.getZ(), 1.3F, 4.0F + Math.abs(spread));
            world.spawnEntity(projectile);
        }
        this.playSound(SoundEvents.ENTITY_BLAZE_SHOOT, 1.4F, 0.7F);
    }

    /** Gummy tendrils: pulls the player in and spawns helpers. */
    private void summon(ServerWorld world, int phase) {
        int count = phase >= 5 ? 4 : 2;
        for (int i = 0; i < count; i++) {
            InfectionRuntime.spawnMonster(world, this.getBlockPos().add(this.random.nextInt(11) - 5, 0, this.random.nextInt(11) - 5), true);
        }
        this.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 1.6F, 0.8F);
        world.spawnParticles(CandyParticles.GUMMY_DROPLET, this.getX(), this.getY() + 2.0D, this.getZ(), 60, 2.0D, 2.0D, 2.0D, 0.05D);
    }

    /** Caramel traps placed under and around the player. */
    private void caramelTraps(ServerWorld world, LivingEntity target) {
        for (int i = 0; i < 8; i++) {
            BlockPos pos = target.getBlockPos().add(this.random.nextInt(7) - 3, -1, this.random.nextInt(7) - 3);
            net.minecraft.block.BlockState state = world.getBlockState(pos);
            if (!state.isAir()) {
                dev.candyinfection.infection.InfectionSpread.convert(world, pos, state,
                        dev.candyinfection.init.CandyBlocks.CARAMEL_GROWTH.getDefaultState());
            }
        }
        this.playSound(SoundEvents.BLOCK_HONEY_BLOCK_PLACE, 1.4F, 0.7F);
    }

    /** Sugar crystal spikes: instant area damage around the boss. */
    private void sugarSpikes(ServerWorld world) {
        Box box = this.getBoundingBox().expand(7.0D);
        for (PlayerEntity player : world.getEntitiesByClass(PlayerEntity.class, box, entity -> true)) {
            player.damage(this.getDamageSources().magic(), 6.0F);
            PlayerInfection.add(player, 4.0F);
        }
        world.playSound(null, this.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.HOSTILE, 2.0F, 0.6F);
        world.spawnParticles(CandyParticles.SUGAR_SPARKLE, this.getX(), this.getY() + 1.0D, this.getZ(), 90, 5.0D, 1.0D, 5.0D, 0.06D);
    }

    /** Chocolate explosion: blast plus infection. */
    private void chocolateExplosion(ServerWorld world) {
        dev.candyinfection.infection.CandyBlast.explode(world, this, this.getX(), this.getY(), this.getZ(),
                7.0F, 14.0F, 5.0F);
        InfectionConversions.infectArea(world, this.getBlockPos(), 7.0D, 0.85F, InfectionStagesHolder.stage(world), this.random);
    }

    /** Infection wave: converts a large disc of terrain. */
    private void infectionWave(ServerWorld world) {
        int converted = InfectionConversions.infectArea(world, this.getBlockPos(), 12.0D, 0.7F,
                InfectionStagesHolder.stage(world), this.random);
        world.playSound(null, this.getBlockPos(), SoundEvents.BLOCK_SCULK_CATALYST_BLOOM, SoundCategory.HOSTILE, 2.5F, 0.6F);
        world.spawnParticles(CandyParticles.INFECTION_SPARK, this.getX(), this.getY() + 1.0D, this.getZ(),
                160, 9.0D, 2.0D, 9.0D, 0.07D);
        dev.candyinfection.util.CandyLog.debug("Confectioner infection wave converted " + converted + " blocks");
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        // Slight damage resistance while the shell is intact (phase 1).
        if (this.getPhase() == 1 && source.getAttacker() instanceof PlayerEntity) {
            amount *= 0.85F;
        }
        return super.damage(source, amount);
    }

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        this.bossBar.addPlayer(player);
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        this.bossBar.removePlayer(player);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);
        this.bossBar.clearPlayers();
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);
        if (this.getWorld().isClient || !(this.getWorld() instanceof ServerWorld world)) {
            return;
        }
        // Unique boss rewards.
        BlockPos pos = this.getBlockPos();
        drop(world, pos, new ItemStack(CandyItems.CHOCOLATE_CORE, 4));
        drop(world, pos, new ItemStack(CandyItems.HOLY_SUGAR, 3));
        drop(world, pos, new ItemStack(CandyItems.PURIFICATION_CRYSTAL, 4));
        drop(world, pos, new ItemStack(CandyItems.CANDY_ESSENCE, 8));
        drop(world, pos, new ItemStack(CandyItems.CONFECTIONER_TROPHY));

        // The arena starts purifying itself.
        InfectionWorldState.get(world).addPurge(this.getBlockPos(), 64, 9000);
        InfectionWorldState.get(world).setBossSpawned(false);
        world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_WITHER_DEATH, SoundCategory.HOSTILE, 3.0F, 0.8F);
        for (PlayerEntity player : world.getPlayers()) {
            player.sendMessage(Text.translatable("message.candyinfection.boss_defeated"), false);
        }
    }

    private static void drop(ServerWorld world, BlockPos pos, ItemStack stack) {
        world.spawnEntity(new ItemEntity(world, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack));
    }

    @Override
    public int candyColor() {
        return 0xFF1493;
    }

    /** Small helper so the phase logic can read the world stage. */
    private static final class InfectionStagesHolder {
        private static int stage(ServerWorld world) {
            return InfectionWorldState.get(world).getStage();
        }
    }
}
