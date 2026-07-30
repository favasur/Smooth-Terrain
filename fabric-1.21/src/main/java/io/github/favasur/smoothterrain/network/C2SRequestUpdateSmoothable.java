package io.github.favasur.smoothterrain.network;

import io.github.favasur.smoothterrain.SmoothTerrain;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public record C2SRequestUpdateSmoothable(
	boolean newValue,
	BlockState[] states
) implements FabricPacket {
	public static final PacketType<C2SRequestUpdateSmoothable> TYPE = PacketType.create(
		new ResourceLocation(SmoothTerrain.MOD_ID, "requestupdatesmoothable"),
		buf -> SmoothTerrainNetwork.Serializer.decodeUpdateSmoothable(buf, C2SRequestUpdateSmoothable::new)
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
