package io.github.favasur.smoothterrain.compat;

import io.github.favasur.smoothterrain.config.SmoothTerrainConfig;
import io.github.favasur.smoothterrain.config.SmoothTerrainConfigImpl;
import net.minecraft.client.OptionInstance;

/**
 * The Video Settings "Smooth Terrain" toggle, built as a vanilla {@link OptionInstance} so it
 * renders exactly like the neighboring options (e.g. Smooth Lighting): a button labeled
 * "Smooth Terrain: ON/OFF". It drives the same enable/disable path as the Creative Settings
 * world toggle ({@link SmoothTerrainConfigImpl.Server#setEnabled(boolean)}) and refreshes the
 * rendering afterwards so the change is visible immediately.
 */
public final class SmoothTerrainToggleButton {

    private SmoothTerrainToggleButton() {
    }

    public static OptionInstance<Boolean> createOption() {
        return OptionInstance.createBoolean(
                "options.smoothterrain",
                OptionInstance.noTooltip(),
                SmoothTerrainConfig.Client.render,
                value -> {
                    SmoothTerrainConfigImpl.Server.setEnabled(value);
                    SmoothTerrainConfigImpl.refreshRendering();
                });
    }
}
