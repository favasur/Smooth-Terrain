package io.github.favasur.smoothterrain.network;

import io.github.favasur.smoothterrain.SmoothTerrain;
import io.github.favasur.smoothterrain.network.BlockStateStreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record C2SRequestUpdateSmoothable(
	boolean newValue,
	BlockState[] states
) implements CustomPacketPayload {

	public static final Type<C2SRequestUpdateSmoothable> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SmoothTerrain.MOD_ID, "c2s_request_update_smoothable"));

	public static final StreamCodec<RegistryFriendlyByteBuf, C2SRequestUpdateSmoothable> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.BOOL, C2SRequestUpdateSmoothable::newValue,
		BlockStateStreamCodec.STREAM_CODEC, C2SRequestUpdateSmoothable::states,
		C2SRequestUpdateSmoothable::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(C2SRequestUpdateSmoothable msg, IPayloadContext ctx) {
		var sender = (ServerPlayer) ctx.player();
		ctx.enqueueWork(() -> SmoothTerrainNetwork.handleC2SRequestUpdateSmoothable(
			sender, msg.newValue, msg.states,
			runnable -> runnable.run(),
			(playerIfNotNullElseEveryone, newValue, states) -> {
				var packet = new S2CUpdateSmoothable(newValue, states);
				if (playerIfNotNullElseEveryone == null) {
					PacketDistributor.sendToAllPlayers(packet);
				} else {
					PacketDistributor.sendToPlayer(playerIfNotNullElseEveryone, packet);
				}
			}
		));
	}
}
