package io.github.favasur.smoothterrain.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.github.favasur.smoothterrain.hooks.ClientHooks;
import io.github.favasur.smoothterrain.mixin.Constants;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Fabric 1.21.1 version of the canonical {@code RenderChunkRebuildTaskMixin}. The canonical mixin
 * targets the NeoForge-patched 5-arg {@link SectionCompiler#compile} (which takes the additional
 * {@code List<AdditionalSectionRenderer>}); vanilla 1.21.1 (Fabric) only has the 4-arg compile, so
 * the method descriptor differs here. The redirect targets inside compile (getRenderShape,
 * RenderChunkRegion.getBlockState, getFluidState) are identical in both.
 */
@Mixin(SectionCompiler.class)
public abstract class RenderChunkRebuildTaskMixin {

	/**
	 * @see ClientHooks#getRenderShape
	 */
	@Redirect(
		method = "compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/state/BlockState;getRenderShape()Lnet/minecraft/world/level/block/RenderShape;"
		)
	)
	public RenderShape noCubes$getRenderShape(BlockState state) {
		return ClientHooks.getRenderShape(state);
	}

	/**
	 * See documentation on {@link Constants#EXTENDED_FLUIDS_BLOCK_POS_REF_NAME}
	 */
	@ModifyReceiver(
		method = "compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"
		)
	)
	private RenderChunkRegion storeBlockPos(
		RenderChunkRegion renderChunkRegion,
		BlockPos pos, @Share(Constants.EXTENDED_FLUIDS_BLOCK_POS_REF_NAME) LocalRef<BlockPos> posRef
	) {
		posRef.set(pos);
		return renderChunkRegion;
	}

	/**
	 * See documentation on {@link Constants#EXTENDED_FLUIDS_BLOCK_POS_REF_NAME}
	 */
	@Redirect(
		method = "compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/state/BlockState;getFluidState()Lnet/minecraft/world/level/material/FluidState;"
		)
	)
	private FluidState getRenderFluidState(
		BlockState instance,
		@Share(Constants.EXTENDED_FLUIDS_BLOCK_POS_REF_NAME) LocalRef<BlockPos> pos
	) {
		return ClientHooks.getRenderFluidState(pos.get(), instance);
	}
}
