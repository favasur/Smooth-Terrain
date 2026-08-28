package net.neoforged.neoforge.network.registration;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class PayloadRegistrar {
    public PayloadRegistrar versioned(String version) {
        return this;
    }

    public PayloadRegistrar optional() {
        return this;
    }

    public <T extends CustomPacketPayload> PayloadRegistrar playToServer(
            CustomPacketPayload.Type<T> type,
            StreamCodec<?, T> codec,
            java.util.function.BiConsumer<T, IPayloadContext> handler) {
        return this;
    }

    public <T extends CustomPacketPayload> PayloadRegistrar playToClient(
            CustomPacketPayload.Type<T> type,
            StreamCodec<?, T> codec,
            java.util.function.BiConsumer<T, IPayloadContext> handler) {
        return this;
    }

    public <T extends CustomPacketPayload> PayloadRegistrar playBidirectional(
            CustomPacketPayload.Type<T> type,
            StreamCodec<?, T> codec,
            java.util.function.BiConsumer<T, IPayloadContext> handler) {
        return this;
    }
}
