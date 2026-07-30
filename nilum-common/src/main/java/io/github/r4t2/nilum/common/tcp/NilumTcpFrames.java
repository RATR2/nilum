package io.github.r4t2.nilum.common.tcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class NilumTcpFrames {

    private NilumTcpFrames() {
    }

    public static void writeFrame(OutputStream out, byte[] payload) throws IOException {
        DataOutputStream data = new DataOutputStream(out);
        data.writeInt(payload.length);
        data.write(payload);
        data.flush();
    }

    public static byte[] readFrame(InputStream in) throws IOException {
        DataInputStream data = new DataInputStream(in);
        int length = data.readInt();
        byte[] payload = new byte[length];
        data.readFully(payload);
        return payload;
    }
}
