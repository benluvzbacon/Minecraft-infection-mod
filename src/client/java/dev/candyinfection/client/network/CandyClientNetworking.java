package dev.candyinfection.client.network;

import dev.candyinfection.client.CandyClientState;
import dev.candyinfection.network.InfectionSyncPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Receives the infection sync payload and mirrors it into {@link CandyClientState}. */
public final class CandyClientNetworking {
    private CandyClientNetworking() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(InfectionSyncPayload.ID,
                (payload, context) -> CandyClientState.update(payload.infection(), payload.stage(),
                        payload.nearbyInfected()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> CandyClientState.reset());
    }
}
