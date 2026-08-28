package io.github.favasur.smoothterrain.fabric;

import io.github.favasur.smoothterrain.network.C2SRequestUpdateSmoothable;
import io.github.favasur.smoothterrain.network.S2CUpdateServerConfig;
import io.github.favasur.smoothterrain.network.S2CUpdateSmoothable;
import io.github.favasur.smoothterrain.network.SmoothTerrainNetworkClient;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.BiConsumer;

/**
 * Fabric transport for the canonical Smooth Terrain payloads, mirroring {@code
 * SmoothTerrainNetworkNeoForge}. Registers the C2S smoothable-toggle and the two S2C packets
 * (smoothable sync + server config sync), sends the server config to each joining player and wires
 * the client join hook so {@link SmoothTerrainNetworkClient} behaves like the NeoForge build
 * (info message, visuals warning, collisions disabled on servers without Smooth Terrain).
 */
public final class SmoothTerrainNetworkFabric {

    private static boolean typesRegistered;
    private static boolean serverReceiversRegistered;
    private static boolean clientReceiversRegistered;

    private SmoothTerrainNetworkFabric() {
    }

    /** Server-side registration: payload types, the C2S receiver and join hooks. */
    public static synchronized void register() {
        registerTypes();
        if (serverReceiversRegistered) {
            return;
        }
        serverReceiversRegistered = true;

        server(C2SRequestUpdateSmoothable.TYPE, C2SRequestUpdateSmoothable::handle);

        ServerLifecycleEvents.SERVER_STARTED.register(PacketDistributor::setServer);

        // Push the server config to every player that joins so the client knows the server has
        // Smooth Terrain and mirrors its settings (the canonical build does this via config sync).
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            PacketDistributor.setServer(server);
            ServerPlayNetworking.send(handler.getPlayer(), S2CUpdateServerConfig.create());
        });
    }

    /** Client-side registration: payload types, the S2C receivers and the join hook. */
    public static synchronized void registerClient() {
        registerTypes();
        if (clientReceiversRegistered) {
            return;
        }
        clientReceiversRegistered = true;

        client(S2CUpdateSmoothable.TYPE, S2CUpdateSmoothable::handle);
        client(S2CUpdateServerConfig.TYPE, S2CUpdateServerConfig::handle);

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            boolean integrated = client.getSingleplayerServer() != null;
            SmoothTerrainNetworkClient.currentServerHasSmoothTerrain = integrated;
            SmoothTerrainNetworkClient.onJoinedServer(integrated);
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static synchronized void registerTypes() {
        if (typesRegistered) {
            return;
        }
        typesRegistered = true;
        PayloadTypeRegistry.playC2S().register(C2SRequestUpdateSmoothable.TYPE, C2SRequestUpdateSmoothable.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(S2CUpdateSmoothable.TYPE, S2CUpdateSmoothable.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(S2CUpdateServerConfig.TYPE, S2CUpdateServerConfig.STREAM_CODEC);
    }

    private static <T extends CustomPacketPayload> void server(CustomPacketPayload.Type<T> type,
                                                              BiConsumer<T, IPayloadContext> handler) {
        ServerPlayNetworking.registerGlobalReceiver(type,
                (payload, context) -> handler.accept(payload, new ServerContext(context)));
    }

    private static <T extends CustomPacketPayload> void client(CustomPacketPayload.Type<T> type,
                                                              BiConsumer<T, IPayloadContext> handler) {
        ClientPlayNetworking.registerGlobalReceiver(type,
                (payload, context) -> handler.accept(payload, new ClientContext(context)));
    }

    private record ServerContext(ServerPlayNetworking.Context delegate) implements IPayloadContext {
        @Override
        public void enqueueWork(Runnable work) {
            delegate.server().execute(work);
        }

        @Override
        public Player player() {
            return delegate.player();
        }

        @Override
        public PacketFlow flow() {
            return PacketFlow.SERVERBOUND;
        }
    }

    private record ClientContext(ClientPlayNetworking.Context delegate) implements IPayloadContext {
        @Override
        public void enqueueWork(Runnable work) {
            delegate.client().execute(work);
        }

        @Override
        public Player player() {
            return delegate.player();
        }

        @Override
        public PacketFlow flow() {
            return PacketFlow.CLIENTBOUND;
        }
    }
}
