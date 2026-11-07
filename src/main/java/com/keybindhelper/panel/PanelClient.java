package com.keybindhelper.panel;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/** Đăng ký phím mở "Bảng chức năng" (mặc định: K). */
public class PanelClient implements ClientModInitializer {
    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        PanelConfig.load();

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("keybindhelper", "panel"));
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.keybindhelper.open_panel", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    client.setScreen(new PanelScreen());
                }
            }
        });
    }
}
