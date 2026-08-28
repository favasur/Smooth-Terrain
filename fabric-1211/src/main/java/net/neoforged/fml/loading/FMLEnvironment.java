package net.neoforged.fml.loading;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.neoforged.api.distmarker.Dist;

public final class FMLEnvironment {
    public static final Dist dist = FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT
            ? Dist.CLIENT : Dist.DEDICATED_SERVER;

    private FMLEnvironment() {
    }
}
