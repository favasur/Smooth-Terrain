package io.github.favasur.smoothterrain.network;

import io.github.favasur.smoothterrain.SmoothTerrain;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record S2CUpdateServerConfig(
	byte[] data
) implements FabricPacket {
	public static final PacketType<S2CUpdateServerConfig> TYPE = PacketType.create(
		new ResourceLocation(SmoothTerrain.MOD_ID, "syncconfig"),
		buf -> SmoothTerrainNetwork.Serializer.decodeS2CUpdateServerConfig(buf, S2CUpdateServerConfig::new)
	);

	@Override
	public void write(FriendlyByteBuf buf) {
		SmoothTerrainNetwork.Serializer.encodeS2CUpdateServerConfig(buf, data);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
