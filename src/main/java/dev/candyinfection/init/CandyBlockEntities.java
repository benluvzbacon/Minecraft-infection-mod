package dev.candyinfection.init;

import dev.candyinfection.CandyInfection;
import dev.candyinfection.block.entity.InfectionCoreBlockEntity;
import dev.candyinfection.block.entity.PurifierBlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/** Block entity type registration. */
public final class CandyBlockEntities {
    public static final BlockEntityType<InfectionCoreBlockEntity> INFECTION_CORE = register("infection_core",
            BlockEntityType.Builder.create(InfectionCoreBlockEntity::new, CandyBlocks.INFECTION_CORE).build(null));

    public static final BlockEntityType<PurifierBlockEntity> PURIFIER = register("purifier",
            BlockEntityType.Builder.create(PurifierBlockEntity::new, CandyBlocks.PURIFIER).build(null));

    private CandyBlockEntities() {
    }

    private static <T extends net.minecraft.block.entity.BlockEntity> BlockEntityType<T> register(
            String name, BlockEntityType<T> type) {
        return Registry.register(Registries.BLOCK_ENTITY_TYPE, CandyInfection.id(name), type);
    }

    public static void register() {
        dev.candyinfection.util.CandyLog.debug("Block entities initialised");
    }
}
