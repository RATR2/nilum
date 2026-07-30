package io.github.r4t2.nilum.common.tcp;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public final class NilumTcpClient {

    private NilumTcpClient() {
    }

    public static Socket connect(String host, int port, String token, int connectTimeoutMillis) {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), connectTimeoutMillis);
            new DataOutputStream(socket.getOutputStream()).writeUTF(token);
            return socket;
        } catch (IOException e) {
            closeQuietly(socket);
            return null;
        }
    }

    private static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
