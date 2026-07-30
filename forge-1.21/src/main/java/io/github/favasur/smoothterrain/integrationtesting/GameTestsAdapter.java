package io.github.favasur.smoothterrain.integrationtesting;

import io.github.favasur.smoothterrain.SmoothTerrain;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final public class GameTestsAdapter {

	public static Collection<TestFunction> createTests(String emptyStructure, Supplier<Stream<Block>> getAllBlocks) {
		return Arrays.stream(SmoothTerrainTests.createTests(getAllBlocks))
			.map(test -> new TestFunction(
				SmoothTerrain.MOD_ID + "Integration", // batch
				SmoothTerrain.MOD_ID + "_" + test.name().replace(' ', '_'),
				emptyStructure, // structure
				Rotation.NONE,
				20, // maxTicks
				20L, // setupTicks
				true, // required
				helper -> {
					try {
						test.action().run();
					} catch (AssertionError e) {
						helper.fail(e.getMessage());
						return;
					}
					helper.succeed();
				}
			))
			.collect(Collectors.toList());
	}
}
