package dev.candyinfection.init;

import dev.candyinfection.CandyInfection;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;

/** The creative inventory tab holding every candy item. */
public final class CandyItemGroups {
    public static final RegistryKey<ItemGroup> MAIN =
            RegistryKey.of(RegistryKeys.ITEM_GROUP, CandyInfection.id("main"));

    public static final ItemGroup GROUP = FabricItemGroup.builder()
            .icon(() -> new ItemStack(CandyItems.CONFECTIONER_TROPHY))
            .displayName(Text.translatable("itemGroup.candyinfection.main"))
            .entries((context, entries) -> {
                for (net.minecraft.item.Item item : CandyItems.all()) {
                    entries.add(item);
                }
            })
            .build();

    private CandyItemGroups() {
    }

    public static void register() {
        Registry.register(Registries.ITEM_GROUP, MAIN, GROUP);
        dev.candyinfection.util.CandyLog.debug("Creative tab registered");
    }
}
