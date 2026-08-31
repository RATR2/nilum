package io.github.r4t2.nilum.common.tcp;

import io.github.r4t2.nilum.common.protocol.AssetDataPacket;
import io.github.r4t2.nilum.common.protocol.AssetKind;
import io.github.r4t2.nilum.common.protocol.AssetRequestPacket;

import java.io.IOException;
import java.net.Socket;
import java.util.function.BiFunction;

public final class NilumTcpAssetServer {

    private NilumTcpAssetServer() {
    }

    public static void serve(Socket socket, BiFunction<AssetKind, String, byte[]> assetLookup) {
        Thread thread = new Thread(() -> loop(socket, assetLookup), "nilum-tcp-asset-server");
        thread.setDaemon(true);
        thread.start();
    }

    private static void loop(Socket socket, BiFunction<AssetKind, String, byte[]> assetLookup) {
        try {
            while (!socket.isClosed()) {
                AssetRequestPacket request = AssetRequestPacket.decode(
                        NilumTcpFrames.readFrame(socket.getInputStream()));

                byte[] data = assetLookup.apply(request.kind(), request.assetId());
                AssetDataPacket response = data != null
                        ? new AssetDataPacket(request.assetId(), request.kind(), true, data)
                        : new AssetDataPacket(request.assetId(), request.kind(), false, new byte[0]);

                NilumTcpFrames.writeFrame(socket.getOutputStream(), response.encode());
            }
        } catch (IOException ignored) {
            // Socket closed or the connection dropped
        }
    }
}
