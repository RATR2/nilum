package io.github.r4t2.nilum.common.protocol;

public record ModListRequestPacket() {

    public byte[] encode() {
        return new byte[0];
    }

    public static ModListRequestPacket decode(byte[] bytes) {
        return new ModListRequestPacket();
    }
}
