package dev.candyinfection.item;

import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ToolMaterial;

/**
 * Exposes vanilla's mining-tool attribute builder.
 *
 * <p>{@code MiningToolItem.createAttributeModifiers} is protected, and the candy
 * tools want exactly the vanilla numbers (plus their own special bonuses) rather
 * than hand-rolled attribute maps, so this bridge re-publishes it.
 */
public abstract class CandyToolBridge extends PickaxeItem {
    protected CandyToolBridge(ToolMaterial material, Settings settings) {
        super(material, settings);
    }

    public static AttributeModifiersComponent miningAttributes(ToolMaterial material, float attackDamage, float attackSpeed) {
        return createAttributeModifiers(material, attackDamage, attackSpeed);
    }

    /** Convenience for the shared candy tool settings. */
    public static Settings candyToolSettings(ToolMaterial material, float attackDamage, float attackSpeed) {
        return new Settings()
                .maxDamage(material.getDurability())
                .attributeModifiers(miningAttributes(material, attackDamage, attackSpeed));
    }
}
