package io.github.r4t2.nilum.fabric.handshake;

import io.github.r4t2.nilum.common.protocol.HelloPacket;
import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.github.r4t2.nilum.fabric.network.NilumHelloPayload;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ConfigurationTask;

import java.util.function.Consumer;

/**
 * Configuration-stage task that blocks the client from reaching PLAY until the Nilum handshake completes.
 */
public record NilumHandshakeTask(HelloPacket hello) implements ConfigurationTask {

    public static final Type TYPE = new Type(NilumChannels.HELLO_QUALIFIED);

    @Override
    public void start(Consumer<Packet<?>> sender) {
        sender.accept(ServerConfigurationNetworking.createS2CPacket(new NilumHelloPayload(hello.encode())));
    }

    @Override
    public Type type() {
        return TYPE;
    }
}
