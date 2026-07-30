package io.github.favasur.smoothterrain.fabric;

import io.github.favasur.smoothterrain.integrationtesting.GameTestsAdapter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;

import java.util.Collection;

public class GameTests {

	// Refers to './run/gameTestServer/gameteststructures/empty.snbt'
	public static final String EMPTY_STRUCTURE = "empty";

	@GameTestGenerator
	public static Collection<TestFunction> createTests() {
		return GameTestsAdapter.createTests(EMPTY_STRUCTURE, BuiltInRegistries.BLOCK::stream);
	}
}
