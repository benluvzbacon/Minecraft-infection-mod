package dev.candyinfection.network;

import dev.candyinfection.CandyInfection;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * Server to client: the player's current infection level and the world
 * infection stage. This is what drives the on-screen infection meter.
 */
public record InfectionSyncPayload(float infection, int stage, int nearbyInfected) implements CustomPayload {
    public static final CustomPayload.Id<InfectionSyncPayload> ID =
            new CustomPayload.Id<>(CandyInfection.id("infection_sync"));

    public static final PacketCodec<RegistryByteBuf, InfectionSyncPayload> CODEC = PacketCodec.ofStatic(
            InfectionSyncPayload::write, InfectionSyncPayload::read);

    private static void write(RegistryByteBuf buf, InfectionSyncPayload payload) {
        buf.writeFloat(payload.infection());
        buf.writeVarInt(payload.stage());
        buf.writeVarInt(payload.nearbyInfected());
    }

    private static InfectionSyncPayload read(RegistryByteBuf buf) {
        return new InfectionSyncPayload(buf.readFloat(), buf.readVarInt(), buf.readVarInt());
    }

    @Override
    public CustomPayload.Id<InfectionSyncPayload> getId() {
        return ID;
    }
}
