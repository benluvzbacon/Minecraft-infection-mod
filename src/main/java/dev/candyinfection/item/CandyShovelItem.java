package dev.candyinfection.item;

import net.minecraft.item.ShovelItem;
import net.minecraft.item.ToolMaterial;

/** Candy shovel: standard vanilla behaviour with the candy tool tier. */
public class CandyShovelItem extends ShovelItem {
    public CandyShovelItem(ToolMaterial material, Settings settings) {
        super(material, CandyToolBridge.candyToolSettings(material, 1.5F, -3.0F));
    }
}
