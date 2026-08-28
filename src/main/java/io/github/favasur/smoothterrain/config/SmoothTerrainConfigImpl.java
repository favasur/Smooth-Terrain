package io.github.favasur.smoothterrain.config;

import com.google.common.collect.Lists;
import io.github.favasur.smoothterrain.SmoothTerrain;
import io.github.favasur.smoothterrain.collision.CollisionHandler;
import io.github.favasur.smoothterrain.config.SmoothTerrainConfig.Server.MesherType;
import io.github.favasur.smoothterrain.network.S2CUpdateServerConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.*;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import static io.github.favasur.smoothterrain.client.RenderHelper.reloadAllChunks;

public final class SmoothTerrainConfigImpl {

	private static final Logger LOG = LogManager.getLogger();

	public static void register(ModContainer container, IEventBus modBus) {
		var specs = new HashMap<ModConfigSpec, Pair<ModConfig.Type, Consumer<ModConfig>>>();
		specs.put(Common.SPEC, Pair.of(ModConfig.Type.COMMON, Common::bake));
		specs.put(Client.SPEC, Pair.of(ModConfig.Type.CLIENT, Client::bake));
		specs.put(Server.SPEC, Pair.of(ModConfig.Type.SERVER, Server::bake));
		specs.forEach((spec, typeAndBaker) -> container.registerConfig(typeAndBaker.getKey(), spec));
		modBus.addListener((ModConfigEvent event) -> {
			var config = event.getConfig();
			var typeAndBaker = specs.get(config.getSpec());
			if (typeAndBaker != null)
				bakeConfig(config, typeAndBaker.getValue());
		});
	}

	private static void bakeConfig(ModConfig config, Consumer<ModConfig> baker) {
		if (!((ModConfigSpec) config.getSpec()).isLoaded()) return;
		baker.accept(config);
	}

	public static class Common {
		static final Impl INSTANCE;
		static final ModConfigSpec SPEC;
		static { var p = new Builder().configure(Impl::new); SPEC = p.getRight(); INSTANCE = p.getLeft(); }
		static void bake(ModConfig config) { SmoothTerrainConfig.Common.debugEnabled = INSTANCE.debugEnabled.get(); }
		static class Impl {
			final BooleanValue debugEnabled;
			private Impl(Builder b) { debugEnabled = b.define("debugEnabled", false); }
		}
	}

	public static class Client {
		public static String RENDER = "render";
		static final Impl INSTANCE;
		static final ModConfigSpec SPEC;
		static { var p = new Builder().configure(Impl::new); SPEC = p.getRight(); INSTANCE = p.getLeft(); }
		static void bake(ModConfig config) {
			boolean oldRender = SmoothTerrainConfig.Client.render;
			SmoothTerrainConfig.Client.render = SmoothTerrainConfig.Server.forceVisuals || INSTANCE.render.get();
			SmoothTerrainConfig.Client.renderSelectionBox = INSTANCE.renderSelectionBox.get();
			SmoothTerrainConfig.Client.selectionBoxColor = ColorParser.parse(INSTANCE.selectionBoxColor.get()).toRenderableColor();
			SmoothTerrainConfig.Client.betterGrassSides = INSTANCE.betterGrassSides.get();
			SmoothTerrainConfig.Client.moreSnow = INSTANCE.moreSnow.get();
			SmoothTerrainConfig.Client.fixPlantHeight = INSTANCE.fixPlantHeight.get();
			SmoothTerrainConfig.Client.grassTufts = INSTANCE.grassTufts.get();
			if (oldRender != SmoothTerrainConfig.Client.render)
				reloadAllChunks("custom rendering toggled");
			SmoothTerrainConfig.Client.debugOutlineSmoothables = INSTANCE.debugOutlineSmoothables.get();
			SmoothTerrainConfig.Client.debugVisualiseDensitiesGrid = INSTANCE.debugVisualiseDensitiesGrid.get();
			SmoothTerrainConfig.Client.debugRenderCollisions = INSTANCE.debugRenderCollisions.get();
			SmoothTerrainConfig.Client.debugRenderMeshCollisions = INSTANCE.debugRenderMeshCollisions.get();
			SmoothTerrainConfig.Client.debugRecordMeshPerformance = INSTANCE.debugRecordMeshPerformance.get();
			SmoothTerrainConfig.Client.debugOutlineNearbyMesh = INSTANCE.debugOutlineNearbyMesh.get();
			// debugSkipNoCubesRendering not available in this config version
		}
		public static void updateRender(boolean v) { INSTANCE.render.set(v); Hacks.saveAndLoad(ModConfig.Type.CLIENT); }
		static void saveAndLoad() { Hacks.saveAndLoad(ModConfig.Type.CLIENT); }
		static class Impl {
			final BooleanValue render, renderSelectionBox, betterGrassSides, moreSnow, fixPlantHeight, grassTufts;
			final ConfigValue<String> selectionBoxColor;
			final BooleanValue debugOutlineSmoothables, debugVisualiseDensitiesGrid, debugRenderCollisions, debugRenderMeshCollisions, debugRecordMeshPerformance, debugOutlineNearbyMesh, debugSkipNoCubesRendering;
			private Impl(Builder b) {
				render = b.define("render", false); // Builder Tools: smooth terrain is opt-in via the world setting
				renderSelectionBox = b.define("renderSelectionBox", true);
				selectionBoxColor = b.define("selectionBoxColor", "#0006");
				betterGrassSides = b.define("betterGrassSides", false);
				moreSnow = b.define("moreSnow", false);
				fixPlantHeight = b.define("fixPlantHeight", false);
				grassTufts = b.define("grassTufts", false);
				b.push("debug");
				debugOutlineSmoothables = b.define("debugOutlineSmoothables", false);
				debugVisualiseDensitiesGrid = b.define("debugVisualiseDensitiesGrid", false);
				debugRenderCollisions = b.define("debugRenderCollisions", false);
				debugRenderMeshCollisions = b.define("debugRenderMeshCollisions", false);
				debugRecordMeshPerformance = b.define("debugRecordMeshPerformance", false);
				debugOutlineNearbyMesh = b.define("debugOutlineNearbyMesh", false);
				debugSkipNoCubesRendering = b.define("debugSkipNoCubesRendering", false);
				b.pop();
			}
		}
	}

	public static class Server {
		static final Impl INSTANCE;
		static final ModConfigSpec SPEC;
		static { var p = new Builder().configure(Impl::new); SPEC = p.getRight(); INSTANCE = p.getLeft(); }
		static void bake(ModConfig config) {
			var blocks = BuiltInRegistries.BLOCK.stream();
			SmoothTerrainConfig.Smoothables.recomputeInMemoryLookup(
				blocks, INSTANCE.smoothableWhitelist.get(), INSTANCE.smoothableBlacklist.get(), INSTANCE.useDefaultSmoothableList.get());
			SmoothTerrainConfig.Server.mesher = INSTANCE.mesher.get().instance;
			SmoothTerrainConfig.Server.collisionsEnabled = INSTANCE.collisionsEnabled.get();
			SmoothTerrainConfig.Server.tempMobCollisionsDisabled = INSTANCE.tempMobCollisionsDisabled.get();
			SmoothTerrainConfig.Server.oldStyleCollisionsEnhancementLevel = INSTANCE.oldStyleCollisionsEnhancementLevel.get();
			SmoothTerrainConfig.Server.onlyOldStyleCollisions = INSTANCE.onlyOldStyleCollisions.get();
			SmoothTerrainConfig.Server.forceVisuals = INSTANCE.forceVisuals.get();
			if (SmoothTerrainConfig.Server.forceVisuals) SmoothTerrainConfig.Client.render = true;
			SmoothTerrainConfig.Server.extendFluidsRange = INSTANCE.extendFluidsRange.get();
			SmoothTerrainConfig.Server.oldSmoothTerrainRoughness = 1F - INSTANCE.terrainSmoothness.get().floatValue();
			// old terrain settings apply automatically
			if (FMLEnvironment.dist.isClient()) reloadAllChunks("server config changed");
			if (FMLEnvironment.dist.isDedicatedServer() && ServerLifecycleHooks.getCurrentServer() != null)
				PacketDistributor.sendToAllPlayers(S2CUpdateServerConfig.create(config));
		}
		public static void updateSmoothable(boolean v, BlockState... s) {
			SmoothTerrainConfig.Smoothables.updateUserDefinedSmoothableStringLists(v, s, (List) INSTANCE.smoothableWhitelist.get(), (List) INSTANCE.smoothableBlacklist.get());
			saveAndLoad();
		}

		/**
		 * The world-setting toggle (Builder Tools Creative Settings): enables or disables Smooth
		 * Terrain meshing at runtime - visuals and collisions. Keeps the in-memory config values in
		 * sync with the baked statics and re-meshes every chunk on the client.
		 */
		public static void setEnabled(boolean enabled) {
			INSTANCE.collisionsEnabled.set(enabled);
			INSTANCE.forceVisuals.set(enabled);
			INSTANCE.mesher.set(MesherType.SurfaceNets);
			SmoothTerrainConfig.Server.mesher = MesherType.SurfaceNets.instance;
			SmoothTerrainConfig.Server.collisionsEnabled = enabled;
			SmoothTerrainConfig.Server.forceVisuals = enabled;
			if (enabled) {
				SmoothTerrainConfig.Smoothables.recomputeInMemoryLookup(
					BuiltInRegistries.BLOCK.stream(), INSTANCE.smoothableWhitelist.get(),
					INSTANCE.smoothableBlacklist.get(), INSTANCE.useDefaultSmoothableList.get());
			}
			SmoothTerrainConfig.Client.render = enabled;
			if (FMLEnvironment.dist.isClient()) {
				reloadAllChunks("smooth terrain " + (enabled ? "enabled" : "disabled"));
			}
			saveAndLoad();
		}

		/**
		 * Runtime smoothness (extent of smoothness) update from the Video Settings slider: 1 is
		 * perfectly smooth, 0 is maximally rough. Persists the spec value and applies it to the
		 * in-memory setting the meshers read (stored inverted as roughness); the actual chunk
		 * re-mesh happens in refreshRendering.
		 */
		public static void setSmoothness(float smoothness) {
			float clamped = Math.max(0F, Math.min(1F, smoothness));
			INSTANCE.terrainSmoothness.set((double) clamped);
			SmoothTerrainConfig.Server.oldSmoothTerrainRoughness = 1F - clamped;
		}
		static void saveAndLoad() { Hacks.saveAndLoad(ModConfig.Type.SERVER); }
		static class Impl {
			final ConfigValue<List<? extends String>> smoothableWhitelist, smoothableBlacklist;
			final BooleanValue useDefaultSmoothableList, collisionsEnabled, tempMobCollisionsDisabled, onlyOldStyleCollisions, forceVisuals, oldNoCubesSlopes, oldNoCubesInFluids;
			final IntValue oldStyleCollisionsEnhancementLevel, extendFluidsRange;
			final DoubleValue terrainSmoothness;
			final EnumValue<MesherType> mesher;
			private Impl(Builder b) {
				smoothableWhitelist = b.defineListAllowEmpty(Collections.singletonList("smoothableWhitelist"), Lists::newArrayList, String.class::isInstance);
				smoothableBlacklist = b.defineListAllowEmpty(Collections.singletonList("smoothableBlacklist"), Lists::newArrayList, String.class::isInstance);
				useDefaultSmoothableList = b.define("useDefaultSmoothableList", true);
				collisionsEnabled = b.define("collisionsEnabled", false); // Builder Tools: opt-in via the world setting
				tempMobCollisionsDisabled = b.define("tempMobCollisionsDisabled", false);
				oldStyleCollisionsEnhancementLevel = b.defineInRange("oldStyleCollisionsEnhancementLevel", 0, 0, CollisionHandler.OLD_COLLISIONS_ENHANCEMENT_LEVEL_MAX);
				onlyOldStyleCollisions = b.define("onlyOldStyleCollisions", false);
				mesher = b.defineEnum("meshGenerator", MesherType.SurfaceNets);
				forceVisuals = b.define("forceVisuals", false);
				extendFluidsRange = b.defineInRange("extendFluidsRange", 1, 0, 2);
				oldNoCubesSlopes = b.define("oldNoCubesSlopes", true);
				oldNoCubesInFluids = b.define("oldNoCubesInFluids", true);
				terrainSmoothness = b.defineInRange("TerrainSmoothness", 0.5F, 0F, 1F);
			}
		}
	}

	/** Re-renders every chunk so a runtime setting change (Video Settings) is visible immediately. */
	public static void refreshRendering() {
		reloadAllChunks("smooth terrain rendering refreshed");
	}

	public static class Hacks {
		static void saveAndLoad(ModConfig.Type type) {
			// Minimal save-and-load stub for NeoForge
			LOG.debug("Config {} save requested", type.name());
		}
		public static void loadDefaultServerConfig() {
			LOG.debug("Loading default server config");
		}
		public static void receiveSyncedServerConfig(byte[] configData) {
			LOG.debug("Received synced server config");
			assert FMLEnvironment.dist.isClient();
		}
	}
}
