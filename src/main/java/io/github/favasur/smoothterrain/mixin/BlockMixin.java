package io.github.favasur.smoothterrain.mixin;

import io.github.favasur.smoothterrain.hooks.trait.ISmoothTerrainBlockType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BlockBehaviour.class)
public class BlockMixin implements ISmoothTerrainBlockType {

	@Final
	@Shadow
	protected boolean hasCollision;

	@Override
	public boolean noCubes$hasCollision() {
		return hasCollision;
	}
}
