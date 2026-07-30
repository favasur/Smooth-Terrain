package io.github.favasur.smoothterrain.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.ConfigParser;
import com.google.common.collect.Lists;
import io.github.favasur.smoothterrain.SmoothTerrain;
import io.github.favasur.smoothterrain.collision.CollisionHandler;
import io.github.favasur.smoothterrain.config.SmoothTerrainConfig.Server.MesherType;
import io.github.favasur.smoothterrain.network.SmoothTerrainNetworkNeoForge;
import io.github.favasur.smoothterrain.network.S2CUpdateServerConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.fml.config.IConfigEvent;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.*;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static io.github.favasur.smoothterrain.client.RenderHelper.reloadAllChunks;

/**
 * Handles registering and baking the configs for NeoForge.
 *
 * @see SmoothTerrainConfig
 */
public final class SmoothTerrainConfigImpl {

	private static final Logger LOG = LogManager.getLogger();

	/**
	 * Called from inside the mod constructor.
	 */
	public static void register(ModContainer container, IEventBus modBus) {
		var specs = new HashMap<ModConfigSpec, Pair<ModConfig.Type, Consumer<ModConfig>>>();
		specs.put(Common.SPEC, Pair.of(ModConfig.Type.COMMON, Common::bake));
		specs.put(Client.SPEC, Pair.of(ModConfig.Type.CLIENT, Client::bake));
		specs.put(Server.SPEC, Pair.of(ModConfig.Type.SERVER, Server::bake));
		specs.forEach((spec, typeAndBaker) -> container.registerConfig(typeAndBaker.getKey(), spec));
		modBus.addListener((ModConfigEvent event) -> {
			var config = event.getConfig();
			var typeAndBaker = specs.get(config.getSpec());
			if (typeAndBaker == null)
				LOG.debug("Received config event for unknown config {}", config.getFileName());
			else
				bakeConfig(config, typeAndBaker.getValue());
		});
	}

	private static void bakeConfig(ModConfig config, Consumer<ModConfig> baker) {
		if (!((ModConfigSpec) config.getSpec()).isLoaded()) {
			LOG.debug("Not baking unloaded config {}", config.getFileName());
			return;
		}
		LOG.debug("Baking config {}", config.getFileName());
		baker.accept(config);
	}

	public static class Common {
		static final Impl INSTANCE;
		static final ModConfigSpec SPEC;

		static {
			var specPair = new Builder().configure(Impl::new);
			SPEC = specPair.getRight();
			INSTANCE = specPair.getLeft();
		}

		static void bake(ModConfig config) {
			SmoothTerrainConfig.Common.debugEnabled = INSTANCE.debugEnabled.get();
		}

		static class Impl {
			final BooleanValue debugEnabled;

			private Impl(Builder builder) {
				debugEnabled = builder
					.translation(SmoothTerrain.MOD_ID + ".config.debugEnabled")
					.comment("If debugging features should be enabled")
					.define("debugEnabled", false);
			}
		}
	}

	public static class Client {
		public static final String INFO_MESSAGE = "infoMessage";
		public static String RENDER = "render";

		static final Impl INSTANCE;
		static final ModConfigSpec SPEC;

		static {
			var specPair = new Builder().configure(Impl::new);
			SPEC = specPair.getRight();
			INSTANCE = specPair.getLeft();
		}

		static void bake(ModConfig config) {
			boolean oldRender = SmoothTerrainConfig.Client.render;
			int oldChunkRenderSettingsHash = SmoothTerrainConfig.Client.hashChunkRenderSettings();

			SmoothTerrainConfig.Client.infoMessage = INSTANCE.infoMessage.get();
			SmoothTerrainConfig.Client.render = SmoothTerrainConfig.Server.forceVisuals || INSTANCE.render.get();
			SmoothTerrainConfig.Client.renderSelectionBox = INSTANCE.renderSelectionBox.get();
			SmoothTerrainConfig.Client.selectionBoxColor = ColorParser.parse(INSTANCE.selectionBoxColor.get()).toRenderableColor();
			SmoothTerrainConfig.Client.betterGrassSides = INSTANCE.betterGrassSides.get();
			SmoothTerrainConfig.Client.moreSnow = INSTANCE.moreSnow.get();
			SmoothTerrainConfig.Client.fixPlantHeight = INSTANCE.fixPlantHeight.get();
			SmoothTerrainConfig.Client.grassTufts = INSTANCE.grassTufts.get();

			if (oldRender != SmoothTerrainConfig.Client.render)
				reloadAllChunks("custom rendering was toggled to %b in the client config", SmoothTerrainConfig.Client.render);
			else if (SmoothTerrainConfig.Client.render && oldChunkRenderSettingsHash != SmoothTerrainConfig.Client.hashChunkRenderSettings())
				reloadAllChunks("options affecting chunk rendering in the client config were changed");

			SmoothTerrainConfig.Client.debugOutlineSmoothables = INSTANCE.debugOutlineSmoothables.get();
			SmoothTerrainConfig.Client.debugVisualiseDensitiesGrid = INSTANCE.debugVisualiseDensitiesGrid.get();
			SmoothTerrainConfig.Client.debugRenderCollisions = INSTANCE.debugRenderCollisions.get();
			SmoothTerrainConfig.Client.debugRenderMeshCollisions = INSTANCE.debugRenderMeshCollisions.get();
			SmoothTerrainConfig.Client.debugRecordMeshPerformance = INSTANCE.debugRecordMeshPerformance.get();
			SmoothTerrainConfig.Client.debugOutlineNearbyMesh = INSTANCE.debugOutlineNearbyMesh.get();
			SmoothTerrainConfig.Client.debugSkipSmoothTerrainRendering = INSTANCE.debugSkipSmoothTerrainRendering.get();
		}

		public static void updateRender(boolean newValue) {
			Client.INSTANCE.render.set(newValue);
			saveAndLoad();
		}

		static void saveAndLoad() {
			Hacks.saveAndLoad(ModConfig.Type.CLIENT);
		}

		static class Impl {
			final BooleanValue infoMessage;
			final BooleanValue render;
			final BooleanValue renderSelectionBox;
			final ConfigValue<String> selectionBoxColor;
			final BooleanValue betterGrassSides;
			final BooleanValue moreSnow;
			final BooleanValue fixPlantHeight;
			final BooleanValue grassTufts;

			final BooleanValue debugOutlineSmoothables;
			final BooleanValue debugVisualiseDensitiesGrid;
			final BooleanValue debugRenderCollisions;
			final BooleanValue debugRenderMeshCollisions;
			final BooleanValue debugRecordMeshPerformance;
			final BooleanValue debugOutlineNearbyMesh;
			final BooleanValue debugSkipSmoothTerrainRendering;

			private Impl(Builder builder) {
				infoMessage = builder
					.translation(SmoothTerrain.MOD_ID + ".config.infoMessage")
					.comment("If SmoothTerrain should display a helpful message when you join a world")
					.define(INFO_MESSAGE, true);

				render = builder
					.translation(SmoothTerrain.MOD_ID + ".config.render")
					.comment("If SmoothTerrain' custom rendering is enabled")
					.define(RENDER, true);

				renderSelectionBox = builder
					.translation(SmoothTerrain.MOD_ID + ".config.renderSelectionBox")
					.comment("If SmoothTerrain' should render a custom outline (selection box) for smoothed blocks")
					.define("renderSelectionBox", true);
				selectionBoxColor = builder
					.translation(SmoothTerrain.MOD_ID + ".config.selectionBoxColor")
					.comment("The color of the outline (selection box) over a smoothed block.")
					.define("selectionBoxColor", "#0006");

				betterGrassSides = builder
					.translation(SmoothTerrain.MOD_ID + ".config.betterGrassSides")
					.comment("Similar to OptiFine's 'Better Grass' feature")
					.define("betterGrassSides", false);

				moreSnow = builder
					.translation(SmoothTerrain.MOD_ID + ".config.moreSnow")
					.comment("Similar to OptiFine's 'Better Snow' feature")
					.define("moreSnow", false);

				fixPlantHeight = builder
					.translation(SmoothTerrain.MOD_ID + ".config.fixPlantHeight")
					.comment("If small plants like flowers and grass should be moved onto SmoothTerrain' terrain")
					.define("fixPlantHeight", false);

				grassTufts = builder
					.translation(SmoothTerrain.MOD_ID + ".config.grassTufts")
					.comment("If small tufts of grass should be rendered on top of grass blocks")
					.define("grassTufts", false);

				builder.push("debug");
				{
					final var debugComment = "Enable debug mode in the common config";
					debugOutlineSmoothables = builder.comment(debugComment).define("debugOutlineSmoothables", false);
					debugVisualiseDensitiesGrid = builder.comment(debugComment).define("debugVisualiseDensitiesGrid", false);
					debugRenderCollisions = builder.comment(debugComment).define("debugRenderCollisions", false);
					debugRenderMeshCollisions = builder.comment(debugComment).define("debugRenderMeshCollisions", false);
					debugRecordMeshPerformance = builder.comment(debugComment).define("debugRecordMeshPerformance", false);
					debugOutlineNearbyMesh = builder.comment(debugComment).define("debugOutlineNearbyMesh", false);
					debugSkipSmoothTerrainRendering = builder.comment(debugComment).define("debugSkipSmoothTerrainRendering", false);
				}
				builder.pop();
			}
		}
	}

	public static class Server {
		static final Impl INSTANCE;
		static final ModConfigSpec SPEC;

		static {
			var specPair = new Builder().configure(Impl::new);
			SPEC = specPair.getRight();
			INSTANCE = specPair.getLeft();
		}

		static void bake(ModConfig config) {
			var blocks = BuiltInRegistries.BLOCK.iterator();
			int oldChunkRenderSettingsHash = SmoothTerrainConfig.Server.hashChunkRenderSettings(
				java.util.stream.StreamSupport.stream(
					((Iterable<?>) () -> BuiltInRegistries.BLOCK.iterator()).spliterator(), false
				)
			);

			SmoothTerrainConfig.Smoothables.recomputeInMemoryLookup(
				java.util.stream.StreamSupport.stream(
					((Iterable<?>) () -> BuiltInRegistries.BLOCK.iterator()).spliterator(), false
				),
				INSTANCE.smoothableWhitelist.get(), INSTANCE.smoothableBlacklist.get(),
				INSTANCE.useDefaultSmoothableList.get()
			);
			SmoothTerrainConfig.Server.mesher = INSTANCE.mesher.get().instance;
			SmoothTerrainConfig.Server.collisionsEnabled = INSTANCE.collisionsEnabled.get();
			SmoothTerrainConfig.Server.tempMobCollisionsDisabled = INSTANCE.tempMobCollisionsDisabled.get();
			SmoothTerrainConfig.Server.oldStyleCollisionsEnhancementLevel = INSTANCE.oldStyleCollisionsEnhancementLevel.get();
			SmoothTerrainConfig.Server.onlyOldStyleCollisions = INSTANCE.onlyOldStyleCollisions.get();
			SmoothTerrainConfig.Server.forceVisuals = INSTANCE.forceVisuals.get();
			if (SmoothTerrainConfig.Server.forceVisuals)
				SmoothTerrainConfig.Client.render = true;
			SmoothTerrainConfig.Server.extendFluidsRange = validateRange(0, 2, INSTANCE.extendFluidsRange.get(), "extendFluidsRange");
			SmoothTerrainConfig.Server.oldSmoothTerrainSlopes = INSTANCE.oldSmoothTerrainSlopes.get();
			SmoothTerrainConfig.Server.oldSmoothTerrainInFluids = INSTANCE.oldSmoothTerrainInFluids.get();
			SmoothTerrainConfig.Server.oldSmoothTerrainRoughness = validateRange(0d, 1d, INSTANCE.oldSmoothTerrainRoughness.get(), "oldSmoothTerrainRoughness").floatValue();

			if (SmoothTerrainConfig.Client.render && oldChunkRenderSettingsHash != SmoothTerrainConfig.Server.hashChunkRenderSettings(
				java.util.stream.StreamSupport.stream(
					((Iterable<?>) () -> BuiltInRegistries.BLOCK.iterator()).spliterator(), false
				)
			))
				if (FMLEnvironment.dist.isClient())
					reloadAllChunks("options affecting chunk rendering in the server config were changed");
			if (FMLEnvironment.dist.isDedicatedServer() && ServerLifecycleHooks.getCurrentServer() != null)
				PacketDistributor.sendToAllPlayers(S2CUpdateServerConfig.create(config));
		}

		static <T extends Number & Comparable<T>> T validateRange(T min, T max, T value, String name) {
			if (value.compareTo(min) < 0 || value.compareTo(max) > 0)
				throw new IllegalStateException("Config was not validated! '" + name + "' must be between " + min + " and " + max + " but was " + value);
			return value;
		}

		public static void updateSmoothable(boolean newValue, BlockState... states) {
			SmoothTerrainConfig.Smoothables.updateUserDefinedSmoothableStringLists(newValue, states, (List) INSTANCE.smoothableWhitelist.get(), (List) INSTANCE.smoothableBlacklist.get());
			saveAndLoad();
		}

		static void saveAndLoad() {
			Hacks.saveAndLoad(ModConfig.Type.SERVER);
		}

		static class Impl {
			final ConfigValue<List<? extends String>> smoothableWhitelist;
			final ConfigValue<List<? extends String>> smoothableBlacklist;
			final BooleanValue useDefaultSmoothableList;
			final EnumValue<MesherType> mesher;
			final BooleanValue collisionsEnabled;
			final BooleanValue tempMobCollisionsDisabled;
			final IntValue oldStyleCollisionsEnhancementLevel;
			final BooleanValue onlyOldStyleCollisions;
			final BooleanValue forceVisuals;
			final IntValue extendFluidsRange;
			final BooleanValue oldSmoothTerrainSlopes;
			final BooleanValue oldSmoothTerrainInFluids;
			final DoubleValue oldSmoothTerrainRoughness;

			private Impl(Builder builder) {
				smoothableWhitelist = builder
					.translation(SmoothTerrain.MOD_ID + ".config.smoothableWhitelist")
					.comment("What blocks should be smoothed by SmoothTerrain")
					.defineListAllowEmpty(Collections.singletonList("smoothableWhitelist"), Lists::newArrayList, String.class::isInstance);

				smoothableBlacklist = builder
					.translation(SmoothTerrain.MOD_ID + ".config.smoothableBlacklist")
					.comment("What blocks should not be smoothed by SmoothTerrain")
					.defineListAllowEmpty(Collections.singletonList("smoothableBlacklist"), Lists::newArrayList, String.class::isInstance);

				useDefaultSmoothableList = builder
					.translation(SmoothTerrain.MOD_ID + ".config.useDefaultSmoothableList")
					.comment("If SmoothTerrain should smooth common natural blocks")
					.define("useDefaultSmoothableList", true);

				collisionsEnabled = builder
					.translation(SmoothTerrain.MOD_ID + ".config.collisionsEnabled")
					.comment("If players should be able to walk up the smooth slopes")
					.define("collisionsEnabled", true);

				tempMobCollisionsDisabled = builder
					.translation(SmoothTerrain.MOD_ID + ".config.tempMobCollisionsDisabled")
					.comment("If ONLY players should be able to walk up smooth slopes")
					.define("tempMobCollisionsDisabled", false);

				oldStyleCollisionsEnhancementLevel = builder
					.translation(SmoothTerrain.MOD_ID + ".config.oldStyleCollisionsEnhancementLevel")
					.comment("Set to a value higher than 0 for old collisions system")
					.defineInRange("oldStyleCollisionsEnhancementLevel", 0, 0, CollisionHandler.OLD_COLLISIONS_ENHANCEMENT_LEVEL_MAX);

				onlyOldStyleCollisions = builder
					.translation(SmoothTerrain.MOD_ID + ".config.onlyOldStyleCollisions")
					.comment("If ONLY the old-style collision algorithm should be used")
					.define("onlyOldStyleCollisions", false);

				mesher = builder
					.translation(SmoothTerrain.MOD_ID + ".config.meshGenerator")
					.comment("The algorithm that should be used to smooth terrain")
					.defineEnum("meshGenerator", MesherType.SurfaceNets);

				forceVisuals = builder
					.translation(SmoothTerrain.MOD_ID + ".config.forceVisuals")
					.comment("For MMO servers that require SmoothTerrain to be enabled")
					.define("forceVisuals", false);

				extendFluidsRange = builder
					.translation(SmoothTerrain.MOD_ID + ".config.extendFluidsRange")
					.comment("The range at which to extend fluids into smoothable blocks")
					.defineInRange("extendFluidsRange", 1, 0, 2);

				oldSmoothTerrainSlopes = builder
					.translation(SmoothTerrain.MOD_ID + ".config.oldSmoothTerrainSlopes")
					.comment("If slopes should be featured in the mesh generated by OldSmoothTerrain")
					.define("oldSmoothTerrainSlopes", true);

				oldSmoothTerrainInFluids = builder
					.translation(SmoothTerrain.MOD_ID + ".config.oldSmoothTerrainInFluids")
					.comment("If slopes should be generated inside fluids by OldSmoothTerrain")
					.define("oldSmoothTerrainInFluids", true);

				oldSmoothTerrainRoughness = builder
					.translation(SmoothTerrain.MOD_ID + ".config.oldSmoothTerrainRoughness")
					.comment("How much pseudo-random roughness should be applied")
					.defineInRange("oldSmoothTerrainRoughness", 0.5F, 0F, 1F);
			}
		}
	}

	public static class Hacks {
		static void saveAndLoad(ModConfig.Type type) {
			LOG.debug("Saving and loading {} config", type.name());
			ConfigTracker_getConfig(type).ifPresent(modConfig -> {
				LOG.debug("Found {} ModConfig to save and load", type.name());
				modConfig.save();
				loadConfig(modConfig);
			});
		}

		static void loadConfig(ModConfig modConfig) {
			((CommentedFileConfig) modConfig.getConfigData()).load();
			modConfig.getSpec().afterReload();
			ModConfig_fireEvent(modConfig, IConfigEvent.reloading(modConfig));
		}

		public static void loadDefaultServerConfig() {
			LOG.debug("Loading default server config");
			ConfigTracker_getConfig(ModConfig.Type.SERVER).ifPresent(modConfig -> {
				LOG.debug("Found ModConfig to load as default");
				var config = CommentedConfig.inMemory();
				modConfig.getSpec().correct(config);
				ModConfig_setConfigData(modConfig, config);
				ModConfig_fireEvent(modConfig, IConfigEvent.loading(modConfig));
			});
		}

		private static Optional<ModConfig> ConfigTracker_getConfig(ModConfig.Type type) {
			return ConfigTracker.INSTANCE.configSets().get(type).stream()
				.filter(modConfig -> modConfig.getModId().equals(SmoothTerrain.MOD_ID))
				.findFirst();
		}

		private static void ModConfig_setConfigData(ModConfig modConfig, CommentedConfig data) {
			try {
				var setConfigData = ObfuscationReflectionHelper.findMethod(ModConfig.class, "setConfigData", CommentedConfig.class);
				setConfigData.invoke(modConfig, data);
			} catch (IllegalAccessException | InvocationTargetException e) {
				throw new RuntimeException("Could not set config data for config " + modConfig, e);
			}
		}

		private static void ModConfig_fireEvent(ModConfig modConfig, IConfigEvent event) {
			ModList.get().getModContainerById(modConfig.getModId()).get().dispatchConfigEvent(event);
		}

		public static void receiveSyncedServerConfig(byte[] configData) {
			LOG.debug("Setting logical server config (on the client) from server sync packet");
			assert FMLEnvironment.dist.isClient() : "This packet should have only be sent server->client";
			var modConfig = ConfigTracker_getConfig(ModConfig.Type.SERVER).get();
			var parser = (ConfigParser<CommentedConfig>) modConfig.getConfigData().configFormat().createParser();
			ModConfig_setConfigData(modConfig, parser.parse(new ByteArrayInputStream(configData)));
			ModConfig_fireEvent(modConfig, IConfigEvent.reloading(modConfig));
		}
	}
}
