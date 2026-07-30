package io.github.r4t2.nilum.common.asset;

import io.github.r4t2.nilum.common.logging.NilumLogger;
import io.github.r4t2.nilum.common.protocol.AssetManifestEntry;

import java.io.IOException;
import java.net.Socket;
import java.util.List;


public final class AssetSyncSession {

    private final AssetCache cache;
    private final ClientModelStore modelStore;
    private final NilumLogger logger;

    private volatile Socket tcpSocket;
    private volatile List<AssetManifestEntry> pendingManifest;
    private boolean fetching;

    public AssetSyncSession(AssetCache cache, ClientModelStore modelStore, NilumLogger logger) {
        this.cache = cache;
        this.modelStore = modelStore;
        this.logger = logger;
    }

    public synchronized void onManifest(List<AssetManifestEntry> entries) {
        this.pendingManifest = entries;
        tryFetch();
    }

    public synchronized void onTcpConnected(Socket socket) {
        this.tcpSocket = socket;
        tryFetch();
    }

    public synchronized void onTcpDisconnected() {
        this.tcpSocket = null;
    }

    private void tryFetch() {
        if (fetching) {
            return;
        }
        Socket socket = tcpSocket;
        List<AssetManifestEntry> manifest = pendingManifest;
        if (socket == null || manifest == null) {
            return;
        }

        fetching = true;
        Thread.ofVirtual().start(() -> {
            fetchAll(socket, manifest);
            synchronized (AssetSyncSession.this) {
                fetching = false;
            }
        });
    }

    private void fetchAll(Socket socket, List<AssetManifestEntry> manifest) {
        for (AssetManifestEntry entry : manifest) {
            if (socket.isClosed()) {
                return;
            }
            try {
                byte[] data;
                if (cache.isCached(entry.assetId(), entry.sha256())) {
                    data = cache.read(entry.assetId());
                } else {
                    data = AssetClient.request(socket, entry.assetId());
                    cache.write(entry.assetId(), data);
                    logger.info("Cached asset '" + entry.assetId() + "' (" + data.length + " bytes).");
                }
                modelStore.load(entry.assetId(), data);
            } catch (IOException e) {
                logger.warn("Failed to fetch asset '" + entry.assetId() + "': " + e);
            } catch (RuntimeException e) {
                logger.warn("Failed to parse asset '" + entry.assetId() + "': " + e);
            }
        }
    }
}
