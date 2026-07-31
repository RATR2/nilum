package io.github.r4t2.nilum.common.protocol;

public final class NilumChannels {

    public static final String NAMESPACE = "nilum";

    public static final String HELLO = "hello";
    public static final String HELLO_QUALIFIED = NAMESPACE + ":" + HELLO;

    public static final String HELLO_ACK = "hello_ack";
    public static final String HELLO_ACK_QUALIFIED = NAMESPACE + ":" + HELLO_ACK;

    public static final String TCP_OFFER = "tcp_offer";
    public static final String TCP_OFFER_QUALIFIED = NAMESPACE + ":" + TCP_OFFER;

    public static final String TCP_UNAVAILABLE = "tcp_unavailable";
    public static final String TCP_UNAVAILABLE_QUALIFIED = NAMESPACE + ":" + TCP_UNAVAILABLE;

    public static final String MODEL_SPAWN = "model_spawn";
    public static final String MODEL_SPAWN_QUALIFIED = NAMESPACE + ":" + MODEL_SPAWN;

    public static final String ASSET_MANIFEST = "asset_manifest";
    public static final String ASSET_MANIFEST_QUALIFIED = NAMESPACE + ":" + ASSET_MANIFEST;

    public static final String MOD_LIST_REQUEST = "mod_list_request";
    public static final String MOD_LIST_REQUEST_QUALIFIED = NAMESPACE + ":" + MOD_LIST_REQUEST;

    public static final String MOD_LIST = "mod_list";
    public static final String MOD_LIST_QUALIFIED = NAMESPACE + ":" + MOD_LIST;

    private NilumChannels() {
    }
}
