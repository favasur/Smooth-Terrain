package io.github.favasur.smoothterrain.network;

import io.github.favasur.smoothterrain.SmoothTerrain;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public record S2CUpdateSmoothable(
	boolean newValue,
	BlockState[] states
) implements FabricPacket {
	public static final PacketType<S2CUpdateSmoothable> TYPE = PacketType.create(
		new ResourceLocation(SmoothTerrain.MOD_ID, "updatesmoothable"),
		buf -> SmoothTerrainNetwork.Serializer.decodeUpdateSmoothable(buf, S2CUpdateSmoothable::new)
	);

	@Override
	public void write(FriendlyByteBuf buf) {
		SmoothTerrainNetwork.Serializer.encodeUpdateSmoothable(buf, newValue, states);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
