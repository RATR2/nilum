package io.github.r4t2.nilum.common.protocol;

/** Tells the client to restore whatever Iris shaderpack state it had before Nilum activated one. */
public record DeactivateShaderPackPacket() {

    public byte[] encode() {
        return new byte[0];
    }

    public static DeactivateShaderPackPacket decode(byte[] bytes) {
        return new DeactivateShaderPackPacket();
    }
}
