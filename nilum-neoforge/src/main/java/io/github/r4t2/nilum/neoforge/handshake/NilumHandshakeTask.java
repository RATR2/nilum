package io.github.r4t2.nilum.neoforge.handshake;

import io.github.r4t2.nilum.common.protocol.HelloPacket;
import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.github.r4t2.nilum.neoforge.network.NilumHelloPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.network.ConfigurationTask;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;

import java.util.function.Consumer;

/**
 * Configuration-stage task that blocks the client from reaching PLAY until the Nilum handshake completes.
 */
public record NilumHandshakeTask(HelloPacket hello) implements ICustomConfigurationTask {

    public static final ConfigurationTask.Type TYPE = new ConfigurationTask.Type(NilumChannels.HELLO_QUALIFIED);

    @Override
    public void run(Consumer<CustomPacketPayload> sender) {
        sender.accept(new NilumHelloPayload(hello.encode()));
    }

    @Override
    public ConfigurationTask.Type type() {
        return TYPE;
    }
}
