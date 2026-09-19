package dev.candyinfection.client.hud;

import dev.candyinfection.client.CandyClientState;
import dev.candyinfection.config.CandyConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * The on-screen infection meter.
 *
 * <p>It only appears when it says something: the player is infected, or they are
 * standing in Infected Land. Otherwise the HUD stays out of the way, and the
 * whole thing can be switched off in the config.
 */
public final class InfectionHud {
    private static final int BAR_WIDTH = 104;
    private static final int BAR_HEIGHT = 6;

    private InfectionHud() {
    }

    public static void register() {
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> render(drawContext));
    }

    private static void render(DrawContext context) {
        if (!CandyConfig.get().hudEnabled) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.player.isSpectator() || client.getDebugHud().shouldShowDebugHud()) {
            return;
        }
        float infection = CandyClientState.infection();
        int nearby = CandyClientState.nearbyInfected();
        boolean infected = infection >= 0.5F;
        boolean inColony = nearby >= 64;
        if (!infected && !inColony) {
            return;
        }

        int x = client.getWindow().getScaledWidth() / 2 - BAR_WIDTH / 2;
        int y = client.getWindow().getScaledHeight() - 46;

        // Frame.
        context.fill(x - 2, y - 10, x + BAR_WIDTH + 2, y + BAR_HEIGHT + 2, 0x66000000);
        // Empty track.
        context.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0x44FFFFFF);
        // Filled portion, colour ramping pink -> magenta -> red.
        if (infected) {
            int width = Math.round(BAR_WIDTH * Math.min(100.0F, infection) / 100.0F);
            context.fill(x, y, x + width, y + BAR_HEIGHT, barColor(infection));
        }

        String label = "INFECTION: " + Math.round(infection) + "%";
        context.drawTextWithShadow(client.textRenderer, label, x, y - 9, 0xFFE14F);
        String stage = "STAGE: " + CandyClientState.stageLabel();
        context.drawTextWithShadow(client.textRenderer, stage, x + BAR_WIDTH - client.textRenderer.getWidth(stage),
                y - 9, 0xFF69B4);
        if (inColony) {
            context.drawTextWithShadow(client.textRenderer, "INFECTED LAND", x, y + BAR_HEIGHT + 3, 0xFF99CC);
        }
    }

    private static int barColor(float infection) {
        if (infection < 25.0F) {
            return 0xFFFF69B4;
        }
        if (infection < 50.0F) {
            return 0xFFFF1493;
        }
        if (infection < 75.0F) {
            return 0xFFC71585;
        }
        return 0xFFFF3355;
    }
}
