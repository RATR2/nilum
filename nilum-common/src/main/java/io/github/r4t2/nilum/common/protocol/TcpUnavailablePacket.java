package io.github.r4t2.nilum.common.protocol;

public record TcpUnavailablePacket() {

    public byte[] encode() {
        return new byte[0];
    }

    public static TcpUnavailablePacket decode(byte[] bytes) {
        return new TcpUnavailablePacket();
    }
}
