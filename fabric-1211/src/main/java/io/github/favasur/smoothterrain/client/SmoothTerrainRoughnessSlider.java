package io.github.favasur.smoothterrain.client;

import io.github.favasur.smoothterrain.config.SmoothTerrainConfig;
import io.github.favasur.smoothterrain.config.SmoothTerrainConfigImpl;
import net.minecraft.client.OptionInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * The Video Settings "Terrain Smoothness" slider, built as a vanilla {@link OptionInstance} so it
 * renders like the other sliders in the screen. The slider ranges from 0.1 (minimum smoothness,
 * left) to 1.0 (maximum smoothness, right) with 0.5 as the default. The value is written to the
 * shared {@link SmoothTerrainConfig.Server#oldSmoothTerrainRoughness} setting (stored inverted)
 * live while dragging, and all chunks are re-rendered afterwards so the change is visible
 * immediately.
 */
public final class SmoothTerrainRoughnessSlider {

    private SmoothTerrainRoughnessSlider() {
    }

    public static OptionInstance<Double> createOption() {
        double current = Mth.clamp(1.0 - SmoothTerrainConfig.Server.oldSmoothTerrainRoughness, 0.1, 1.0);
        return new OptionInstance<>(
                "options.smoothterrain.smoothness",
                OptionInstance.noTooltip(),
                (caption, value) -> Component.translatable(
                        "options.percent_value", caption, (int) Math.round(value * 100.0)),
                OptionInstance.UnitDouble.INSTANCE.xmap(
                        slider -> 0.1 + slider * 0.9,
                        smoothness -> (smoothness - 0.1) / 0.9),
                current,
                value -> {
                    SmoothTerrainConfigImpl.Server.setSmoothness(value.floatValue());
                    SmoothTerrainConfigImpl.refreshRendering();
                });
    }
}
