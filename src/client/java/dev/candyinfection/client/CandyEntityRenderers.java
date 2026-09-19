package dev.candyinfection.client;

import dev.candyinfection.CandyInfection;
import dev.candyinfection.client.render.entity.CandyMobRenderer;
import dev.candyinfection.entity.CandyCrawlerEntity;
import dev.candyinfection.entity.CandyHostileEntity;
import dev.candyinfection.entity.CandyProjectileEntity;
import dev.candyinfection.init.CandyEntities;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.client.render.entity.model.CreeperEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.model.SlimeEntityModel;
import net.minecraft.client.render.entity.model.SpiderEntityModel;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;

/** Registers a renderer for every candy entity. */
public final class CandyEntityRenderers {
    private CandyEntityRenderers() {
    }

    public static void register() {
        EntityRendererRegistry.register(CandyEntities.CANDY_CRAWLER, context ->
                spider(context, new SpiderEntityModel<>(context.getPart(EntityModelLayers.SPIDER)), 0.6F, "candy_crawler"));
        EntityRendererRegistry.register(CandyEntities.GUMMY_SPAWN, context ->
                slime(context, new SlimeEntityModel<>(context.getPart(EntityModelLayers.SLIME)), 0.3F, "gummy_spawn"));
        EntityRendererRegistry.register(CandyEntities.GUMMY_BRUTE, context ->
                biped(context, new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), false), 0.7F, "gummy_brute"));
        EntityRendererRegistry.register(CandyEntities.SUGAR_LEECH, context ->
                slime(context, new SlimeEntityModel<>(context.getPart(EntityModelLayers.SLIME)), 0.3F, "sugar_leech"));
        EntityRendererRegistry.register(CandyEntities.CANDY_MIMIC, context ->
                creeper(context, new CreeperEntityModel<>(context.getPart(EntityModelLayers.CREEPER)), 0.4F, "candy_mimic"));
        EntityRendererRegistry.register(CandyEntities.CARAMEL_BEAST, context ->
                biped(context, new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), false), 0.7F, "caramel_beast"));
        EntityRendererRegistry.register(CandyEntities.JAWBREAKER, context ->
                slime(context, new SlimeEntityModel<>(context.getPart(EntityModelLayers.SLIME)), 0.4F, "jawbreaker"));
        EntityRendererRegistry.register(CandyEntities.CHOCOLATE_CREEPER, context ->
                creeper(context, new CreeperEntityModel<>(context.getPart(EntityModelLayers.CREEPER)), 0.4F, "chocolate_creeper"));
        EntityRendererRegistry.register(CandyEntities.LOLLIPOP_STALKER, context ->
                biped(context, new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), true), 0.5F, "lollipop_stalker"));
        EntityRendererRegistry.register(CandyEntities.CONFECTIONER, context ->
                biped(context, new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), false), 0.9F, "the_confectioner"));
        EntityRendererRegistry.register(CandyEntities.CANDY_PROJECTILE,
                context -> new FlyingItemEntityRenderer<CandyProjectileEntity>(context, 1.2F, false));
    }

    private static <T extends CandyCrawlerEntity> CandyMobRenderer<T, SpiderEntityModel<T>> spider(
            EntityRendererFactory.Context context, SpiderEntityModel<T> model, float shadow, String texture) {
        return new CandyMobRenderer<>(context, model, shadow, texture(texture));
    }

    private static <T extends MobEntity> CandyMobRenderer<T, SlimeEntityModel<T>> slime(
            EntityRendererFactory.Context context, SlimeEntityModel<T> model, float shadow, String texture) {
        return new CandyMobRenderer<>(context, model, shadow, texture(texture));
    }

    private static <T extends MobEntity> CandyMobRenderer<T, CreeperEntityModel<T>> creeper(
            EntityRendererFactory.Context context, CreeperEntityModel<T> model, float shadow, String texture) {
        return new CandyMobRenderer<>(context, model, shadow, texture(texture));
    }

    private static <T extends CandyHostileEntity> CandyMobRenderer<T, PlayerEntityModel<T>> biped(
            EntityRendererFactory.Context context, PlayerEntityModel<T> model, float shadow, String texture) {
        return new CandyMobRenderer<>(context, model, shadow, texture(texture));
    }

    private static Identifier texture(String name) {
        return CandyInfection.id("textures/entity/" + name + ".png");
    }
}
