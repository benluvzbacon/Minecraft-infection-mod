package dev.candyinfection.client.render.entity;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;

/**
 * One renderer for every candy monster.
 *
 * <p>They share a renderer because the differences between the monsters are
 * their behaviour, scale and texture, not their skeleton; the vanilla model
 * skeletons give readable silhouettes at no cost, and each monster gets its own
 * brightly coloured texture.
 */
public class CandyMobRenderer<T extends MobEntity, M extends EntityModel<T>> extends MobEntityRenderer<T, M> {
    private final Identifier texture;

    public CandyMobRenderer(EntityRendererFactory.Context context, M model, float shadowRadius, Identifier texture) {
        super(context, model, shadowRadius);
        this.texture = texture;
    }

    @Override
    public Identifier getTexture(T entity) {
        return this.texture;
    }
}
