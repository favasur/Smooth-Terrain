package io.github.favasur.smoothterrain.forge;

import io.github.favasur.smoothterrain.SmoothTerrain;
import io.github.favasur.smoothterrain.integrationtesting.GameTestsAdapter;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collection;

@GameTestHolder(SmoothTerrain.MOD_ID)
public class GameTests {

	// Refers to './run/gameTestServer/gameteststructures/empty.snbt'
	public static final String EMPTY_STRUCTURE = new ResourceLocation(SmoothTerrain.MOD_ID, "empty").toString();

	@GameTestGenerator
	public static Collection<TestFunction> createTests() {
		return GameTestsAdapter.createTests(EMPTY_STRUCTURE, () -> ForgeRegistries.BLOCKS.getValues().stream());
	}
}
