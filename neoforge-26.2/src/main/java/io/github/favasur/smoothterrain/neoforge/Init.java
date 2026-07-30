package io.github.favasur.smoothterrain.neoforge;

import io.github.favasur.smoothterrain.SmoothTerrain;
import io.github.favasur.smoothterrain.config.SmoothTerrainConfigImpl;
import io.github.favasur.smoothterrain.network.SmoothTerrainNetworkNeoForge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(SmoothTerrain.MOD_ID)
public class Init {
	public Init(IEventBus modBus, ModContainer container) {
		register(modBus, container);
	}

	public static void register(IEventBus modBus, ModContainer container) {
		SmoothTerrainConfigImpl.register(container, modBus);
		if (FMLEnvironment.dist.isClient())
			ClientInit.register(modBus);
		SmoothTerrainNetworkNeoForge.register(modBus);
	}
}
