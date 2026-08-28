package net.neoforged.neoforge.network.event;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class RegisterPayloadHandlersEvent {
    public PayloadRegistrar registrar(String version) {
        return new PayloadRegistrar();
    }
}
