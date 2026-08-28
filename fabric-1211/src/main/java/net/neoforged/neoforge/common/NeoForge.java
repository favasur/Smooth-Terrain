package net.neoforged.neoforge.common;

import net.neoforged.bus.api.FabricEventBus;
import net.neoforged.bus.api.IEventBus;

/**
 * The game (non-mod) event bus. On Fabric this is a real bus fed by
 * {@code net.buildertools.FabricHooks} and the client mixins.
 */
public final class NeoForge {
    public static final IEventBus EVENT_BUS = new FabricEventBus();

    private NeoForge() {
    }
}
