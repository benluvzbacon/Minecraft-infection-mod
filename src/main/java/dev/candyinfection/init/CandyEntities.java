package dev.candyinfection.init;

import dev.candyinfection.CandyInfection;
import dev.candyinfection.entity.CandyCrawlerEntity;
import dev.candyinfection.entity.CandyProjectileEntity;
import dev.candyinfection.entity.CaramelBeastEntity;
import dev.candyinfection.entity.ChocolateCreeperEntity;
import dev.candyinfection.entity.ConfectionerEntity;
import dev.candyinfection.entity.GummyBruteEntity;
import dev.candyinfection.entity.GummySpawnEntity;
import dev.candyinfection.entity.JawbreakerEntity;
import dev.candyinfection.entity.LollipopStalkerEntity;
import dev.candyinfection.entity.SugarLeechEntity;
import dev.candyinfection.entity.CandyMimicEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity type registration. Client renderers live in the client source set and
 * are registered from {@code CandyInfectionClient}.
 */
public final class CandyEntities {
    private static final List<EntityType<?>> ALL = new ArrayList<>();

    public static final EntityType<CandyCrawlerEntity> CANDY_CRAWLER = register("candy_crawler",
            EntityType.Builder.create(CandyCrawlerEntity::new, SpawnGroup.MONSTER)
                    .dimensions(1.4F, 0.9F).maxTrackingRange(10).build("candy_crawler"));

    public static final EntityType<GummySpawnEntity> GUMMY_SPAWN = register("gummy_spawn",
            EntityType.Builder.create(GummySpawnEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.6F, 0.5F).maxTrackingRange(8).build("gummy_spawn"));

    public static final EntityType<GummyBruteEntity> GUMMY_BRUTE = register("gummy_brute",
            EntityType.Builder.create(GummyBruteEntity::new, SpawnGroup.MONSTER)
                    .dimensions(1.6F, 3.0F).maxTrackingRange(12).build("gummy_brute"));

    public static final EntityType<SugarLeechEntity> SUGAR_LEECH = register("sugar_leech",
            EntityType.Builder.create(SugarLeechEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.7F, 0.4F).maxTrackingRange(8).build("sugar_leech"));

    public static final EntityType<CandyMimicEntity> CANDY_MIMIC = register("candy_mimic",
            EntityType.Builder.create(CandyMimicEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.9F, 0.9F).maxTrackingRange(8).build("candy_mimic"));

    public static final EntityType<CaramelBeastEntity> CARAMEL_BEAST = register("caramel_beast",
            EntityType.Builder.create(CaramelBeastEntity::new, SpawnGroup.MONSTER)
                    .dimensions(1.5F, 2.4F).maxTrackingRange(12).build("caramel_beast"));

    public static final EntityType<JawbreakerEntity> JAWBREAKER = register("jawbreaker",
            EntityType.Builder.create(JawbreakerEntity::new, SpawnGroup.MONSTER)
                    .dimensions(1.1F, 1.1F).maxTrackingRange(10).build("jawbreaker"));

    public static final EntityType<ChocolateCreeperEntity> CHOCOLATE_CREEPER = register("chocolate_creeper",
            EntityType.Builder.create(ChocolateCreeperEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.7F, 1.7F).maxTrackingRange(10).build("chocolate_creeper"));

    public static final EntityType<LollipopStalkerEntity> LOLLIPOP_STALKER = register("lollipop_stalker",
            EntityType.Builder.create(LollipopStalkerEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.9F, 3.4F).maxTrackingRange(14).build("lollipop_stalker"));

    public static final EntityType<ConfectionerEntity> CONFECTIONER = register("the_confectioner",
            EntityType.Builder.create(ConfectionerEntity::new, SpawnGroup.MONSTER)
                    .dimensions(2.6F, 5.2F).maxTrackingRange(20).makeFireImmune().build("the_confectioner"));

    public static final EntityType<CandyProjectileEntity> CANDY_PROJECTILE = register("candy_projectile",
            EntityType.Builder.<CandyProjectileEntity>create(CandyProjectileEntity::new, SpawnGroup.MISC)
                    .dimensions(0.35F, 0.35F).maxTrackingRange(4).trackingTickInterval(10).build("candy_projectile"));

    private CandyEntities() {
    }

    private static <T extends net.minecraft.entity.Entity> EntityType<T> register(String name, EntityType<T> type) {
        ALL.add(type);
        return Registry.register(Registries.ENTITY_TYPE, CandyInfection.id(name), type);
    }

    public static void register() {
        FabricDefaultAttributeRegistry.register(CANDY_CRAWLER, CandyCrawlerEntity.createCrawlerAttributes());
        FabricDefaultAttributeRegistry.register(GUMMY_SPAWN, GummySpawnEntity.createGummySpawnAttributes());
        FabricDefaultAttributeRegistry.register(GUMMY_BRUTE, GummyBruteEntity.createBruteAttributes());
        FabricDefaultAttributeRegistry.register(SUGAR_LEECH, SugarLeechEntity.createLeechAttributes());
        FabricDefaultAttributeRegistry.register(CANDY_MIMIC, CandyMimicEntity.createMimicAttributes());
        FabricDefaultAttributeRegistry.register(CARAMEL_BEAST, CaramelBeastEntity.createCaramelAttributes());
        FabricDefaultAttributeRegistry.register(JAWBREAKER, JawbreakerEntity.createJawbreakerAttributes());
        FabricDefaultAttributeRegistry.register(CHOCOLATE_CREEPER, ChocolateCreeperEntity.createCreeperAttributes());
        FabricDefaultAttributeRegistry.register(LOLLIPOP_STALKER, LollipopStalkerEntity.createStalkerAttributes());
        FabricDefaultAttributeRegistry.register(CONFECTIONER, ConfectionerEntity.createConfectionerAttributes());
    }

    public static List<EntityType<?>> all() {
        return ALL;
    }

    /** Every entity type that counts towards the candy monster cap. */
    public static List<EntityType<? extends MobEntity>> monsters() {
        List<EntityType<? extends MobEntity>> monsters = new ArrayList<>();
        for (EntityType<?> type : ALL) {
            if (type != CANDY_PROJECTILE) {
                @SuppressWarnings("unchecked")
                EntityType<? extends MobEntity> cast = (EntityType<? extends MobEntity>) type;
                monsters.add(cast);
            }
        }
        return monsters;
    }

    /** Registry names of every candy monster, for command completion. */
    public static List<String> monsterNames() {
        List<String> names = new java.util.ArrayList<>();
        for (EntityType<? extends MobEntity> type : monsters()) {
            names.add(net.minecraft.registry.Registries.ENTITY_TYPE.getId(type).toString());
        }
        return names;
    }

    public static int count() {
        return ALL.size();
    }
}
