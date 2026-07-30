package io.github.favasur.smoothterrain.network;

import io.github.favasur.smoothterrain.SmoothTerrain;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Stores the mod's channel and registers the messages using NeoForge's new payload system.
 */
public final class SmoothTerrainNetworkNeoForge {

	private static final Logger LOG = LogManager.getLogger();

	public static void register(IEventBus modBus) {
		modBus.addListener((RegisterPayloadHandlersEvent event) -> {
			var registrar = event.registrar(SmoothTerrainNetwork.NETWORK_PROTOCOL_VERSION);
			registerC2S(registrar);
			registerS2C(registrar);
		});
	}

	private static void registerC2S(PayloadRegistrar registrar) {
		registrar.playToServer(
			C2SRequestUpdateSmoothable.TYPE,
			C2SRequestUpdateSmoothable.STREAM_CODEC,
			C2SRequestUpdateSmoothable::handle
		);
	}

	private static void registerS2C(PayloadRegistrar registrar) {
		registrar.playToClient(
			S2CUpdateSmoothable.TYPE,
			S2CUpdateSmoothable.STREAM_CODEC,
			S2CUpdateSmoothable::handle
		);
		registrar.playToClient(
			S2CUpdateServerConfig.TYPE,
			S2CUpdateServerConfig.STREAM_CODEC,
			S2CUpdateServerConfig::handle
		);
	}

	public static void sendToServer(C2SRequestUpdateSmoothable msg) {
		PacketDistributor.sendToServer(msg);
	}

	public static void sendToAllPlayers(Object msg) {
		if (msg instanceof S2CUpdateSmoothable smoothable) {
			PacketDistributor.sendToAllPlayers(smoothable);
		} else if (msg instanceof S2CUpdateServerConfig config) {
			PacketDistributor.sendToAllPlayers(config);
		}
	}

	public static void sendToPlayer(net.minecraft.server.level.ServerPlayer player, Object msg) {
		if (msg instanceof S2CUpdateSmoothable smoothable) {
			PacketDistributor.sendToPlayer(player, smoothable);
		} else if (msg instanceof S2CUpdateServerConfig config) {
			PacketDistributor.sendToPlayer(player, config);
		}
	}
}
