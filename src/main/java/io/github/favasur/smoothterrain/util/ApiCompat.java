package io.github.favasur.smoothterrain.util;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.InputStream;

/**
 * Runtime presence probes for Minecraft/loader APIs that changed across the 1.21.x minor versions
 * (1.21.1 … 1.21.11). Each probe checks the actual class/method shape on the running version, so a
 * jar built against 1.21.1 can be loaded by newer 1.21.x loaders: mixins and code paths whose
 * targets no longer exist are skipped instead of crashing the game.
 *
 * <p>Probes inspect class <em>file bytes</em> via {@link ClassLoader#getResourceAsStream} and ASM
 * rather than {@link Class#forName}: loading a Minecraft class from inside a mixin config plugin
 * would re-enter the mixin transformer during its prepare phase and crash the game
 * (ReEntrantTransformerError), while resource lookup is side-effect free and safe both during
 * mixin pre-application and at runtime. All probes are computed lazily once and cached.
 */
public final class ApiCompat {

	private static boolean checked;
	private static boolean sectionCompilerCompileV1;
	private static boolean renderChunkRegion;
	private static boolean modelBakerImplCtor;
	private static boolean blockEntityTypeCtorV1;
	private static boolean breakingTexture5Arg;
	private static boolean renderHitOutlineV1;
	private static boolean renderLevelV1;
	private static boolean setBlockDirtyV1;
	private static boolean weatherMethod;
	private static boolean honeycombMethod;
	private static boolean levelSetBlock4Arg;
	private static boolean liquidBlockRenderer;
	private static boolean isInWallLambda;
	private static boolean itemRendererRenderV1;
	private static boolean startDestroyBlockV1;
	private static boolean debugQuads;
	private static boolean entityVibrationStepSounds;
	private static boolean spawnSprintParticle;
	private static boolean axeEvaluateNewBlockState;
	private static boolean levelChunkSetBlockStateV1;
	private static boolean breakingBlockEffect3Arg;
	private static boolean screenEffectGetOverlayBlock;

	private ApiCompat() {
	}

	public static boolean sectionCompilerCompileV1() {
		check();
		return sectionCompilerCompileV1;
	}

	public static boolean renderChunkRegion() {
		check();
		return renderChunkRegion;
	}

	public static boolean modelBakerImplCtor() {
		check();
		return modelBakerImplCtor;
	}

	public static boolean blockEntityTypeCtorV1() {
		check();
		return blockEntityTypeCtorV1;
	}

	public static boolean breakingTexture5Arg() {
		check();
		return breakingTexture5Arg;
	}

	public static boolean renderHitOutlineV1() {
		check();
		return renderHitOutlineV1;
	}

	public static boolean renderLevelV1() {
		check();
		return renderLevelV1;
	}

	public static boolean setBlockDirtyV1() {
		check();
		return setBlockDirtyV1;
	}

	public static boolean weatherMethod() {
		check();
		return weatherMethod;
	}

	public static boolean honeycombMethod() {
		check();
		return honeycombMethod;
	}

	public static boolean levelSetBlock4Arg() {
		check();
		return levelSetBlock4Arg;
	}

	public static boolean liquidBlockRenderer() {
		check();
		return liquidBlockRenderer;
	}

	public static boolean isInWallLambda() {
		check();
		return isInWallLambda;
	}

	public static boolean itemRendererRenderV1() {
		check();
		return itemRendererRenderV1;
	}

	public static boolean startDestroyBlockV1() {
		check();
		return startDestroyBlockV1;
	}

	public static boolean debugQuads() {
		check();
		return debugQuads;
	}

	public static boolean entityVibrationStepSounds() {
		check();
		return entityVibrationStepSounds;
	}

	public static boolean spawnSprintParticle() {
		check();
		return spawnSprintParticle;
	}

	public static boolean axeEvaluateNewBlockState() {
		check();
		return axeEvaluateNewBlockState;
	}

	public static boolean levelChunkSetBlockStateV1() {
		check();
		return levelChunkSetBlockStateV1;
	}

	/** True on 1.21.2+, where {@code ClientLevel.addBreakingBlockEffect} replaced the 1.21.1 {@code addDestroyBlockEffect}. */
	public static boolean breakingBlockEffect3Arg() {
		check();
		return breakingBlockEffect3Arg;
	}

	public static boolean screenEffectGetOverlayBlock() {
		check();
		return screenEffectGetOverlayBlock;
	}

	private static synchronized void check() {
		if (checked) {
			return;
		}
		checked = true;
		// 1.21.1 vanilla (Fabric) has the 4-arg compile; NeoForge patches in a 5-arg variant with
		// the additional-renderers list. The render mixins target whichever exists.
		sectionCompilerCompileV1 = hasMethod("net.minecraft.client.renderer.chunk.SectionCompiler", "compile", 4)
			|| hasMethod("net.minecraft.client.renderer.chunk.SectionCompiler", "compile", 5);
		renderChunkRegion = classExists("net.minecraft.client.renderer.chunk.RenderChunkRegion");
		modelBakerImplCtor = hasCtor("net.minecraft.client.resources.model.ModelBakery$ModelBakerImpl", 3);
		blockEntityTypeCtorV1 = hasCtor("net.minecraft.world.level.block.entity.BlockEntityType", 3);
		breakingTexture5Arg = hasMethod("net.minecraft.client.renderer.block.BlockRenderDispatcher", "renderBreakingTexture", 5);
		renderHitOutlineV1 = hasMethod("net.minecraft.client.renderer.LevelRenderer", "renderHitOutline", 8);
		renderLevelV1 = hasMethod("net.minecraft.client.renderer.LevelRenderer", "renderLevel", 7);
		setBlockDirtyV1 = hasMethod("net.minecraft.client.renderer.LevelRenderer", "setBlockDirty", 2)
				&& hasMethod("net.minecraft.client.renderer.LevelRenderer", "setBlocksDirty", 6);
		// NeoForge's data-map patch restructures the vanilla private map builders into
		// lambda$static$N methods; probe the names the running game actually contains.
		weatherMethod = hasMethod("net.minecraft.world.level.block.WeatheringCopper", "lambda$static$0", 0);
		honeycombMethod = hasMethod("net.minecraft.world.item.HoneycombItem", "lambda$static$0", 0);
		levelSetBlock4Arg = hasMethod("net.minecraft.world.level.Level", "setBlock", 4);
		liquidBlockRenderer = classExists("net.minecraft.client.renderer.block.LiquidBlockRenderer");
		isInWallLambda = hasMethod("net.minecraft.world.entity.Entity", "lambda$isInWall$8", -1);
		itemRendererRenderV1 = hasMethod("net.minecraft.client.renderer.entity.ItemRenderer", "render", 8)
				&& hasMethod("net.minecraft.client.renderer.entity.ItemRenderer", "getModel", 4);
		startDestroyBlockV1 = hasMethod("net.minecraft.client.multiplayer.MultiPlayerGameMode", "startDestroyBlock", 2);
		debugQuads = hasMethod("net.minecraft.client.renderer.RenderType", "debugQuads", 0);
		entityVibrationStepSounds = hasMethod("net.minecraft.world.entity.Entity", "vibrationAndSoundEffectsFromBlock", 5);
		spawnSprintParticle = hasMethod("net.minecraft.world.entity.Entity", "spawnSprintParticle", 0);
		axeEvaluateNewBlockState = hasMethod("net.minecraft.world.item.AxeItem", "evaluateNewBlockState", 5);
		levelChunkSetBlockStateV1 = hasMethod("net.minecraft.world.level.chunk.LevelChunk", "setBlockState", 3);
		breakingBlockEffect3Arg = hasMethod("net.minecraft.client.multiplayer.ClientLevel", "addBreakingBlockEffect", 3);
		screenEffectGetOverlayBlock = hasMethod("net.minecraft.client.renderer.ScreenEffectRenderer", "getOverlayBlock", -1);
	}

	private static boolean classExists(String name) {
		return readClass(name) != null;
	}

	/** True if a class has a method with the given name and parameter count (-1 = any). */
	private static boolean hasMethod(String className, String methodName, int paramCount) {
		byte[] bytes = readClass(className);
		if (bytes == null) {
			return false;
		}
		try {
			ClassReader reader = new ClassReader(bytes);
			boolean[] found = {false};
			reader.accept(new ClassVisitor(Opcodes.ASM9) {
				@Override
				public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
					if (name.equals(methodName) && (paramCount < 0 || Type.getArgumentTypes(descriptor).length == paramCount)) {
						found[0] = true;
					}
					return null;
				}
			}, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
			return found[0];
		} catch (Throwable t) {
			return false;
		}
	}

	private static boolean hasCtor(String className, int paramCount) {
		byte[] bytes = readClass(className);
		if (bytes == null) {
			return false;
		}
		try {
			ClassReader reader = new ClassReader(bytes);
			boolean[] found = {false};
			reader.accept(new ClassVisitor(Opcodes.ASM9) {
				@Override
				public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
					if (name.equals("<init>") && Type.getArgumentTypes(descriptor).length == paramCount) {
						found[0] = true;
					}
					return null;
				}
			}, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
			return found[0];
		} catch (Throwable t) {
			return false;
		}
	}

	/**
	 * Reads a class file's raw bytes as a resource without loading it. Tries the plugin's own
	 * classloader first, then the context/system classloaders, so Minecraft classes are found both
	 * in the NeoForge/Fabric dev environments and in production module classpaths.
	 */
	private static byte[] readClass(String className) {
		String path = className.replace('.', '/') + ".class";
		ClassLoader[] loaders = {
			ApiCompat.class.getClassLoader(),
			Thread.currentThread().getContextClassLoader(),
			ClassLoader.getSystemClassLoader()
		};
		for (ClassLoader loader : loaders) {
			if (loader == null) {
				continue;
			}
			try (InputStream in = loader.getResourceAsStream(path)) {
				if (in != null) {
					return in.readAllBytes();
				}
			} catch (Throwable ignored) {
				// try the next classloader
			}
		}
		return null;
	}
}
