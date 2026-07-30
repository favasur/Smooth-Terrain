package io.github.favasur.smoothterrain.network;

import io.github.favasur.smoothterrain.util.BlockStateSerializer;
import net.minecraft.core.IdMapper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

/**
 * NeoForge 1.21+ StreamCodec for BlockState arrays.
 * Uses the registry-aware RegistryFriendlyByteBuf.
 */
public final class BlockStateStreamCodec {

	public static final StreamCodec<RegistryFriendlyByteBuf, BlockState[]> STREAM_CODEC = new StreamCodec<>() {
		@Override
		public BlockState[] decode(RegistryFriendlyByteBuf buffer) {
			var count = buffer.readVarInt();
			var states = new BlockState[count];
			for (int i = 0; i < count; i++) {
				states[i] = BlockStateSerializer.fromId(buffer.readVarInt());
			}
			return states;
		}

		@Override
		public void encode(RegistryFriendlyByteBuf buffer, BlockState[] states) {
			buffer.writeVarInt(states.length);
			for (var state : states) {
				buffer.writeVarInt(BlockStateSerializer.toId(state));
			}
		}
	};

	private BlockStateStreamCodec() {}
}
