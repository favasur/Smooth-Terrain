package net.neoforged.bus.api;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Real event bus backing the NeoForge-style shims on Fabric. Listeners registered through
 * {@link #addListener} (typed consumers) or {@link #register} (objects whose
 * {@link SubscribeEvent}-annotated methods are scanned) are dispatched synchronously by
 * {@link #fire} when the Fabric hook layer raises the corresponding game event.
 */
public final class FabricEventBus implements IEventBus {
    private final Map<Class<?>, List<Consumer<?>>> listeners = new HashMap<>();

    @Override
    public synchronized <T> void addListener(Class<T> eventType, Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    }

    @Override
    public synchronized void register(Object listener) {
        Class<?> type = listener instanceof Class<?> clazz ? clazz : listener.getClass();
        for (Method method : type.getDeclaredMethods()) {
            if (method.getAnnotation(SubscribeEvent.class) == null) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params.length != 1) {
                continue;
            }
            Class<?> eventType = params[0];
            method.setAccessible(true);
            if (Modifier.isStatic(method.getModifiers())) {
                listeners.computeIfAbsent(eventType, k -> new ArrayList<>())
                        .add(event -> invoke(method, null, event));
            } else {
                listeners.computeIfAbsent(eventType, k -> new ArrayList<>())
                        .add(event -> invoke(method, listener, event));
            }
        }
    }

    @Override
    public synchronized <E> void fire(E event) {
        List<Consumer<?>> list = listeners.get(event.getClass());
        if (list == null || list.isEmpty()) {
            return;
        }
        for (Consumer<?> consumer : new ArrayList<>(list)) {
            @SuppressWarnings("unchecked")
            Consumer<E> typed = (Consumer<E>) consumer;
            typed.accept(event);
        }
    }

    private static void invoke(Method method, Object target, Object event) {
        try {
            method.invoke(target, event);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to fire @SubscribeEvent handler " + method, e);
        }
    }
}
