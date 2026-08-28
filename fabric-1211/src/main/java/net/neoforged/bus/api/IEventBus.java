package net.neoforged.bus.api;

import java.util.function.Consumer;

/**
 * Event bus contract used by the canonical (NeoForge-flavored) mod code. On Fabric this is
 * backed by {@link FabricEventBus}, which dispatches to the listeners registered through
 * {@link #addListener} or {@link #register} whenever {@link #fire} is invoked from the
 * Fabric hook layer ({@code net.buildertools.FabricHooks} and the mixins).
 */
public interface IEventBus {
    <T> void addListener(Class<T> eventType, Consumer<T> listener);

    void register(Object listener);

    <E> void fire(E event);
}
