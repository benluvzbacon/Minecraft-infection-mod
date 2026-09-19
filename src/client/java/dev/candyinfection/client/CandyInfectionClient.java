package dev.candyinfection.client;

import dev.candyinfection.client.hud.InfectionHud;
import dev.candyinfection.client.network.CandyClientNetworking;
import dev.candyinfection.client.particle.CandyParticleFactories;
import dev.candyinfection.init.CandyBlocks;
import dev.candyinfection.util.CandyLog;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;

/** Client entrypoint: renderers, particles, HUD and the sync payload handler. */
public final class CandyInfectionClient implements ClientModInitializer {
    public CandyInfectionClient() {
    }

    @Override
    public void onInitializeClient() {
        CandyEntityRenderers.register();
        CandyParticleFactories.register();
        CandyClientNetworking.register();
        InfectionHud.register();
        registerRenderLayers();
        CandyLog.phase("Client renderers, particles and HUD ready");
    }

    /**
     * Candy foliage and crystals are drawn with cutout/transparent layers so
     * their textures read as bright, layered confectionery instead of flat
     * opaque cubes.
     */
    private static void registerRenderLayers() {
        BlockRenderLayerMap.INSTANCE.putBlock(CandyBlocks.GUMMY_LEAVES, RenderLayer.getCutoutMipped());
        BlockRenderLayerMap.INSTANCE.putBlock(CandyBlocks.CANDY_VINES, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(CandyBlocks.GUMMY_GROWTH, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(CandyBlocks.CHOCOLATE_GROWTH, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(CandyBlocks.SUGAR_CRYSTAL_CLUSTER, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(CandyBlocks.LOLLIPOP_PINK, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(CandyBlocks.LOLLIPOP_BLUE, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(CandyBlocks.LOLLIPOP_RED, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(CandyBlocks.LOLLIPOP_YELLOW, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(CandyBlocks.HARD_CANDY_PINK, RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putBlock(CandyBlocks.HARD_CANDY_RED, RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putBlock(CandyBlocks.HARD_CANDY_PURPLE, RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putBlock(CandyBlocks.HARD_CANDY_CYAN, RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putBlock(CandyBlocks.SUGAR_CRYSTAL_BLOCK, RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putBlock(CandyBlocks.STICKY_SYRUP, RenderLayer.getTranslucent());
    }
}
