package com.keybindhelper.panel;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Danh sách chức năng có sẵn + phím tự thêm, và cách chạy chúng. */
public final class PanelActions {
    private PanelActions() {}

    public record ActionDef(String id, String label, boolean custom, Consumer<MinecraftClient> run) {}

    public static List<ActionDef> all() {
        List<ActionDef> list = new ArrayList<>();
        list.add(new ActionDef("drop", "Q: Vứt vật phẩm", false,
                mc -> tap(mc.options.dropKey)));
        list.add(new ActionDef("dropStack", "Ctrl+Q: Vứt cả stack", false,
                mc -> {
                    if (mc.player != null) {
                        mc.player.dropSelectedItem(true);
                    }
                }));
        list.add(new ActionDef("swap", "F: Đổi tay", false,
                mc -> tap(mc.options.swapHandsKey)));
        list.add(new ActionDef("inventory", "E: Kho đồ", false,
                mc -> tap(mc.options.inventoryKey)));
        list.add(new ActionDef("chat", "T: Chat", false,
                mc -> tap(mc.options.chatKey)));
        list.add(new ActionDef("perspective", "F5: Góc nhìn", false,
                mc -> tap(mc.options.togglePerspectiveKey)));
        list.add(new ActionDef("pick", "Chuột giữa: Lấy khối", false,
                mc -> tap(mc.options.pickItemKey)));

        for (PanelConfig.CustomKey c : PanelConfig.get().customKeys) {
            String keyText = c.key;
            list.add(new ActionDef(c.id, c.name + " (" + c.key + ")", true,
                    mc -> tapNamed(keyText)));
        }
        return list;
    }

    public static ActionDef find(String id) {
        for (ActionDef a : all()) {
            if (a.id().equals(id)) {
                return a;
            }
        }
        return null;
    }

    /** Giả lập "bấm 1 lần" phím đang gán cho chức năng này. */
    private static void tap(KeyBinding binding) {
        InputUtil.Key key = KeyBindingHelper.getBoundKeyOf(binding);
        KeyBinding.onKeyPressed(key);
    }

    /** Giả lập "bấm 1 lần" một phím theo tên người dùng nhập. */
    private static void tapNamed(String keyText) {
        InputUtil.Key key = resolveKey(keyText);
        if (key != null) {
            KeyBinding.onKeyPressed(key);
        }
    }

    /** Đổi chữ người dùng nhập (vd "r", "f5", "space", "shift") thành phím; trả về null nếu không hợp lệ. */
    public static InputUtil.Key resolveKey(String text) {
        if (text == null) {
            return null;
        }
        String t = text.trim().toLowerCase(Locale.ROOT).replace(' ', '.').replace('_', '.');
        if (t.isEmpty()) {
            return null;
        }
        String full = switch (t) {
            case "shift" -> "key.keyboard.left.shift";
            case "ctrl", "control" -> "key.keyboard.left.control";
            case "alt" -> "key.keyboard.left.alt";
            case "esc" -> "key.keyboard.escape";
            case "lmb", "mouse1" -> "key.mouse.left";
            case "rmb", "mouse2" -> "key.mouse.right";
            case "mmb", "mouse3" -> "key.mouse.middle";
            default -> t.startsWith("key.") ? t : "key.keyboard." + t;
        };
        try {
            return InputUtil.fromTranslationKey(full);
        } catch (RuntimeException e) {
            return null;
        }
    }
                                   }
