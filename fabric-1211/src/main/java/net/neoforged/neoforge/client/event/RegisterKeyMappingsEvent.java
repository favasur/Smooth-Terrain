package net.neoforged.neoforge.client.event;

import net.minecraft.client.KeyMapping;

public final class RegisterKeyMappingsEvent {
    private final java.util.function.Consumer<KeyMapping> registrar;

    public RegisterKeyMappingsEvent() {
        this(key -> { });
    }

    public RegisterKeyMappingsEvent(java.util.function.Consumer<KeyMapping> registrar) {
        this.registrar = registrar;
    }

    public void register(KeyMapping mapping) {
        registrar.accept(mapping);
    }
}
