package com.keybindhelper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Cấu hình lưu tại config/keybindhelper.json */
public final class HelperConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static HelperConfig instance = new HelperConfig();

    /** Đang hiện bảng hay không (phím H / Numpad 0 sẽ đổi giá trị này). */
    public boolean visible = true;

    /** Bật/tắt từng nhóm phím tắt. */
    public boolean showInventory = true;
    public boolean showMovement = true;
    public boolean showDebug = true;
    public boolean showExtra = true;

    /** true = bám góc trên bên phải, false = góc trên bên trái. */
    public boolean anchorRight = false;

    /** Khoảng cách từ mép màn hình (đơn vị: pixel đã nhân GUI Scale). */
    public int offsetX = 4;
    public int offsetY = 4;

    public static HelperConfig get() {
        return instance;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("keybindhelper.json");
    }

    public static void load() {
        Path p = path();
        if (Files.exists(p)) {
            try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                HelperConfig loaded = GSON.fromJson(r, HelperConfig.class);
                if (loaded != null) {
                    instance = loaded;
                }
            } catch (Exception e) {
                System.err.println("[KeybindHelper] Không đọc được config, dùng mặc định: " + e);
            }
        }
        save();
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(path(), StandardCharsets.UTF_8)) {
            GSON.toJson(instance, w);
        } catch (IOException e) {
            System.err.println("[KeybindHelper] Không ghi được config: " + e);
        }
    }
             }
