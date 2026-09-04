package io.github.r4t2.nilum.common.asset;

import io.github.r4t2.nilum.common.logging.NilumLogger;
import io.github.r4t2.nilum.common.protocol.AssetManifestEntry;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;


public final class AssetSyncSession {

    private final AssetCache cache;
    private final ClientModelStore modelStore;
    private final BiConsumer<String, byte[]> iconSink;
    private final BiConsumer<String, byte[]> hudAtlasSink;
    private final BiConsumer<String, byte[]> shaderPackSink;
    private final BiConsumer<String, byte[]> fontSink;
    private final BiConsumer<String, byte[]> customUiSink;
    private final NilumLogger logger;
    private final Executor mainThreadExecutor;

    private volatile Socket tcpSocket;
    private volatile List<AssetManifestEntry> pendingManifest;
    private boolean fetching;

    /**
     * @param mainThreadExecutor runs a sink's actual apply-to-client-state step; several sinks
     *                           (texture upload/invalidate, glyph atlas stitching) touch the GPU,
     *                           which only the render thread is allowed to do, so that step can't
     *                           run inline on fetchAll's own background thread.
     */
    public AssetSyncSession(AssetCache cache, ClientModelStore modelStore, BiConsumer<String, byte[]> iconSink,
                             BiConsumer<String, byte[]> hudAtlasSink, BiConsumer<String, byte[]> shaderPackSink,
                             BiConsumer<String, byte[]> fontSink, BiConsumer<String, byte[]> customUiSink,
                             NilumLogger logger, Executor mainThreadExecutor) {
        this.cache = cache;
        this.modelStore = modelStore;
        this.iconSink = iconSink;
        this.hudAtlasSink = hudAtlasSink;
        this.shaderPackSink = shaderPackSink;
        this.fontSink = fontSink;
        this.customUiSink = customUiSink;
        this.logger = logger;
        this.mainThreadExecutor = mainThreadExecutor;
    }

    public synchronized void onManifest(List<AssetManifestEntry> entries) {
        cache.pruneExcept(entries);
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

            byte[] data;
            try {
                if (cache.isCached(entry.assetId(), entry.kind(), entry.sha256())) {
                    data = cache.read(entry.assetId(), entry.kind());
                } else {
                    data = AssetClient.request(socket, entry.assetId(), entry.kind());
                    cache.write(entry.assetId(), entry.kind(), data);
                    logger.info("Cached asset '" + entry.assetId() + "' (" + data.length + " bytes).");
                }
            } catch (IOException e) {
                logger.warn("Failed to fetch asset '" + entry.assetId() + "'", e);
                continue;
            }

            mainThreadExecutor.execute(() -> applyToSink(entry, data));
        }
    }

    private void applyToSink(AssetManifestEntry entry, byte[] data) {
        try {
            switch (entry.kind()) {
                case MODEL -> modelStore.load(entry.assetId(), data);
                case ICON -> iconSink.accept(entry.assetId(), data);
                case HUD_ATLAS -> hudAtlasSink.accept(entry.assetId(), data);
                case SHADER_PACK -> shaderPackSink.accept(entry.assetId(), data);
                case FONT -> fontSink.accept(entry.assetId(), data);
                case CUSTOM_UI -> customUiSink.accept(entry.assetId(), data);
            }
        } catch (RuntimeException e) {
            logger.warn("Failed to parse asset '" + entry.assetId() + "'", e);
        }
    }
}
