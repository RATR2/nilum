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

    public static final String HUD_FRAME = "hud_frame";
    public static final String HUD_FRAME_QUALIFIED = NAMESPACE + ":" + HUD_FRAME;

    public static final String ATLAS_PATCH = "atlas_patch";
    public static final String ATLAS_PATCH_QUALIFIED = NAMESPACE + ":" + ATLAS_PATCH;

    public static final String HUD_FRAME_OVERRIDE = "hud_frame_override";
    public static final String HUD_FRAME_OVERRIDE_QUALIFIED = NAMESPACE + ":" + HUD_FRAME_OVERRIDE;

    public static final String HUD_FRAME_RELEASE = "hud_frame_release";
    public static final String HUD_FRAME_RELEASE_QUALIFIED = NAMESPACE + ":" + HUD_FRAME_RELEASE;

    public static final String REGISTER_CLIENT_VAR = "register_client_var";
    public static final String REGISTER_CLIENT_VAR_QUALIFIED = NAMESPACE + ":" + REGISTER_CLIENT_VAR;

    public static final String SET_CLIENT_VAR = "set_client_var";
    public static final String SET_CLIENT_VAR_QUALIFIED = NAMESPACE + ":" + SET_CLIENT_VAR;

    private NilumChannels() {
    }
}
