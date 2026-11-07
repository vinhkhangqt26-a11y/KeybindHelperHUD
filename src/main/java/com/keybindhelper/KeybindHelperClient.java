package com.keybindhelper;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class KeybindHelperClient implements ClientModInitializer {
    public static final String MOD_ID = "keybindhelper";

    public static KeyBinding toggleKey;
    private static KeyBinding toggleKeyAlt;

    @Override
    public void onInitializeClient() {
        HelperConfig.load();

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.keybindhelper.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, category));
        toggleKeyAlt = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.keybindhelper.toggle_alt", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_KP_0, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                // F3 + H là phím của game (hiện chi tiết vật phẩm) -> không ẩn/hiện bảng.
                if (!isF3Down(client)) {
                    toggleVisible();
                }
            }
            while (toggleKeyAlt.wasPressed()) {
                toggleVisible();
            }
        });

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.of(MOD_ID, "keybind_hud"),
                HudRenderer::render);
    }

    private static boolean isF3Down(MinecraftClient client) {
        return GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_F3) == GLFW.GLFW_PRESS;
    }

    private static void toggleVisible() {
        HelperConfig cfg = HelperConfig.get();
        cfg.visible = !cfg.visible;
        HelperConfig.save();
    }
    }
