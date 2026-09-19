package dev.candyinfection.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/**
 * Registers every custom payload. Payloads are registered on the common side so
 * that a dedicated server and a client always agree on the channel.
 */
public final class CandyPayloads {
    private CandyPayloads() {
    }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(InfectionSyncPayload.ID, InfectionSyncPayload.CODEC);
    }
}
