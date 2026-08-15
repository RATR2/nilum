package io.github.r4t2.nilum.common.asset;

import io.github.r4t2.nilum.common.protocol.AssetDataPacket;
import io.github.r4t2.nilum.common.protocol.AssetKind;
import io.github.r4t2.nilum.common.protocol.AssetRequestPacket;
import io.github.r4t2.nilum.common.tcp.NilumTcpFrames;

import java.io.IOException;
import java.net.Socket;


public final class AssetClient {

    private AssetClient() {
    }

    public static byte[] request(Socket socket, String assetId, AssetKind kind) throws IOException {
        NilumTcpFrames.writeFrame(socket.getOutputStream(), new AssetRequestPacket(assetId, kind).encode());

        AssetDataPacket response = AssetDataPacket.decode(NilumTcpFrames.readFrame(socket.getInputStream()));
        if (!response.found()) {
            throw new IOException("Server has no asset '" + assetId + "'");
        }
        return response.data();
    }
}
