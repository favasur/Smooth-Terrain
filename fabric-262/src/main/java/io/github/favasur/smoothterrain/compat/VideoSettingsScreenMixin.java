package io.github.favasur.smoothterrain.compat;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Adds the Smooth Terrain toggle button and the Terrain Smoothness slider to the Video Settings
 * screen (the same screen used from the main menu). They are real vanilla {@link OptionInstance}s,
 * spliced into the small-options row list directly under the Smooth Lighting row, so they look and
 * behave exactly like the neighboring options. {@code addOptions} is invoked by
 * {@code OptionsSubScreen#addContents} on every {@code init}, so the injection is safe across
 * window resizes.
 */
@Mixin(VideoSettingsScreen.class)
public abstract class VideoSettingsScreenMixin {

    @Redirect(
            method = "addOptions",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/OptionsList;addSmall([Lnet/minecraft/client/OptionInstance;)V"
            )
    )
    private void smoothterrain$addOptions(OptionsList list, OptionInstance<?>[] options) {
        list.addSmall(buildertools$withSmoothTerrainOptions(options));
    }

    private OptionInstance<?>[] buildertools$withSmoothTerrainOptions(OptionInstance<?>[] options) {
        OptionInstance<Boolean> smoothLighting = Minecraft.getInstance().options.ambientOcclusion();
        int aoIndex = -1;
        for (int i = 0; i < options.length; i++) {
            if (options[i] == smoothLighting) {
                aoIndex = i;
                break;
            }
        }
        if (aoIndex < 0) {
            return options;
        }
        // Insert a new row (two options) directly after the row that contains Smooth Lighting.
        int insertAt = Math.min(options.length, (aoIndex / 2) * 2 + 2);
        OptionInstance<?>[] result = new OptionInstance<?>[options.length + 2];
        System.arraycopy(options, 0, result, 0, insertAt);
        result[insertAt] = SmoothTerrainToggleButton.createOption();
        result[insertAt + 1] = SmoothTerrainRoughnessSlider.createOption();
        System.arraycopy(options, insertAt, result, insertAt + 2, options.length - insertAt);
        return result;
    }
}
