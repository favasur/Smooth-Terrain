package io.github.favasur.smoothterrain.mixin.client;

import io.github.favasur.smoothterrain.client.ClientUtil;
import io.github.favasur.smoothterrain.config.SmoothTerrainConfig;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderChunkRegion.class)
public class RenderChunkRegionMixin {

	/**
	 * Allows us to provide our extended fluids to the vanilla fluid renderer.
	 * Vanilla's implementation uses {@link BlockState#getFluidState()} so we need to change it.
	 */
	@Inject(
		method = "getFluidState",
		at = @At("HEAD"),
		cancellable = true
	)
	public void noCubes$getExtendedFluidState(BlockPos pos, CallbackInfoReturnable<FluidState> ci) {
		if (SmoothTerrainConfig.Server.extendFluidsRange > 0)
			ci.setReturnValue(ClientUtil.getExtendedFluidState(pos));
	}
}
