package io.github.favasur.smoothterrain.network;

import io.github.favasur.smoothterrain.SmoothTerrain;
import io.github.favasur.smoothterrain.network.BlockStateStreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record S2CUpdateSmoothable(
	boolean newValue,
	BlockState[] states
) implements CustomPacketPayload {

	public static final Type<S2CUpdateSmoothable> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SmoothTerrain.MOD_ID, "s2c_update_smoothable"));

	public static final StreamCodec<RegistryFriendlyByteBuf, S2CUpdateSmoothable> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.BOOL, S2CUpdateSmoothable::newValue,
		BlockStateStreamCodec.STREAM_CODEC, S2CUpdateSmoothable::states,
		S2CUpdateSmoothable::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(S2CUpdateSmoothable msg, IPayloadContext ctx) {
		ctx.enqueueWork(() -> SmoothTerrainNetworkClient.handleS2CUpdateSmoothable(
			runnable -> runnable.run(), msg.newValue, msg.states
		));
	}
}
