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

    public static final String SET_HUD_TEXT = "set_hud_text";
    public static final String SET_HUD_TEXT_QUALIFIED = NAMESPACE + ":" + SET_HUD_TEXT;

    public static final String KEYBIND = "keybind";
    public static final String KEYBIND_QUALIFIED = NAMESPACE + ":" + KEYBIND;

    public static final String CHUNK_BLOCKS = "chunk_blocks";
    public static final String CHUNK_BLOCKS_QUALIFIED = NAMESPACE + ":" + CHUNK_BLOCKS;

    public static final String ACTIVATE_SHADER_PACK = "activate_shader_pack";
    public static final String ACTIVATE_SHADER_PACK_QUALIFIED = NAMESPACE + ":" + ACTIVATE_SHADER_PACK;

    public static final String DEACTIVATE_SHADER_PACK = "deactivate_shader_pack";
    public static final String DEACTIVATE_SHADER_PACK_QUALIFIED = NAMESPACE + ":" + DEACTIVATE_SHADER_PACK;

    public static final String ENTITY_ANIMATION_PLAY = "entity_animation_play";
    public static final String ENTITY_ANIMATION_PLAY_QUALIFIED = NAMESPACE + ":" + ENTITY_ANIMATION_PLAY;

    public static final String ENTITY_ANIMATION_STOP = "entity_animation_stop";
    public static final String ENTITY_ANIMATION_STOP_QUALIFIED = NAMESPACE + ":" + ENTITY_ANIMATION_STOP;

    public static final String BLOCK_ANIMATION_PLAY = "block_animation_play";
    public static final String BLOCK_ANIMATION_PLAY_QUALIFIED = NAMESPACE + ":" + BLOCK_ANIMATION_PLAY;

    public static final String BLOCK_ANIMATION_STOP = "block_animation_stop";
    public static final String BLOCK_ANIMATION_STOP_QUALIFIED = NAMESPACE + ":" + BLOCK_ANIMATION_STOP;

    public static final String OPEN_UI = "open_ui";
    public static final String OPEN_UI_QUALIFIED = NAMESPACE + ":" + OPEN_UI;

    public static final String UI_CLOSED = "ui_closed";
    public static final String UI_CLOSED_QUALIFIED = NAMESPACE + ":" + UI_CLOSED;

    public static final String UI_BUTTON_CLICKED = "ui_button_clicked";
    public static final String UI_BUTTON_CLICKED_QUALIFIED = NAMESPACE + ":" + UI_BUTTON_CLICKED;

    public static final String ITEM_DEFINED_ASSETS = "item_defined_assets";
    public static final String ITEM_DEFINED_ASSETS_QUALIFIED = NAMESPACE + ":" + ITEM_DEFINED_ASSETS;

    public static final String SET_HUD_ATLAS_VISIBILITY = "set_hud_atlas_visibility";
    public static final String SET_HUD_ATLAS_VISIBILITY_QUALIFIED = NAMESPACE + ":" + SET_HUD_ATLAS_VISIBILITY;

    public static final String SET_HUD_ELEMENT_VISIBILITY = "set_hud_element_visibility";
    public static final String SET_HUD_ELEMENT_VISIBILITY_QUALIFIED = NAMESPACE + ":" + SET_HUD_ELEMENT_VISIBILITY;

    public static final String ITEM_ANIMATION_PLAY = "item_animation_play";
    public static final String ITEM_ANIMATION_PLAY_QUALIFIED = NAMESPACE + ":" + ITEM_ANIMATION_PLAY;

    public static final String ITEM_ANIMATION_STOP = "item_animation_stop";
    public static final String ITEM_ANIMATION_STOP_QUALIFIED = NAMESPACE + ":" + ITEM_ANIMATION_STOP;

    private NilumChannels() {
    }
}
