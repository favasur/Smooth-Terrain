package net.neoforged.neoforge.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric transport shim for the canonical Smooth Terrain payload records. The canonical code sends
 * through NeoForge's {@code PacketDistributor}; on Fabric the same calls route into
 * {@link ClientPlayNetworking} / {@link ServerPlayNetworking}. The current server is captured by
 * {@link #setServer} (fired on server start and player join) so {@link #sendToAllPlayers} can
 * enumerate its players.
 */
public final class PacketDistributor {

    private static MinecraftServer server;

    private PacketDistributor() {
    }

    public static void setServer(MinecraftServer server) {
        PacketDistributor.server = server;
    }

    public static void sendToServer(Object msg) {
        ClientPlayNetworking.send((CustomPacketPayload) msg);
    }

    public static void sendToAllPlayers(Object msg) {
        MinecraftServer current = server;
        if (current == null) {
            return;
        }
        CustomPacketPayload payload = (CustomPacketPayload) msg;
        for (ServerPlayer player : current.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    public static void sendToPlayer(ServerPlayer player, Object msg) {
        ServerPlayNetworking.send(player, (CustomPacketPayload) msg);
    }
}
