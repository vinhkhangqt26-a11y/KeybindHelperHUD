package com.keybindhelper.panel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Lưu trạng thái các nút chức năng tại config/keybindhelper_panel.json */
public final class PanelConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> DEFAULT_SHOWN = Set.of("drop", "dropStack", "swap");
    private static PanelConfig instance = new PanelConfig();

    /** Trạng thái hiện/ẩn và vị trí của từng nút (theo id chức năng). */
    public static class ButtonState {
        public String id;
        public boolean shown;
        public int x;
        public int y;
    }

    /** Phím do người dùng tự thêm. */
    public static class CustomKey {
        public String id;
        public String name;
        public String key;
    }

    public List<ButtonState> states = new ArrayList<>();
    public List<CustomKey> customKeys = new ArrayList<>();
    public int nextCustomId = 1;
    /** true = bấm nút xong thì đóng bảng rồi mới chạy chức năng. */
    public boolean closeAfterTap = true;

    public static PanelConfig get() {
        return instance;
    }

    /** Lấy trạng thái của một chức năng, tự tạo mới (vị trí mặc định xếp dọc) nếu chưa có. */
    public ButtonState state(String id) {
        for (ButtonState s : states) {
            if (id.equals(s.id)) {
                return s;
            }
        }
        ButtonState s = new ButtonState();
        int count = states.size();
        s.id = id;
        s.shown = DEFAULT_SHOWN.contains(id);
        s.x = 8 + (count / 7) * 90;
        s.y = 30 + (count % 7) * 24;
        states.add(s);
        return s;
    }

    public void addCustom(String name, String key) {
        CustomKey c = new CustomKey();
        c.id = "custom" + nextCustomId;
        nextCustomId++;
        c.name = name;
        c.key = key;
        customKeys.add(c);
        state(c.id).shown = true;
    }

    public void removeCustom(String id) {
        customKeys.removeIf(c -> id.equals(c.id));
        states.removeIf(s -> id.equals(s.id));
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("keybindhelper_panel.json");
    }

    public static void load() {
        Path p = path();
        if (Files.exists(p)) {
            try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                PanelConfig loaded = GSON.fromJson(r, PanelConfig.class);
                if (loaded != null) {
                    if (loaded.states == null) loaded.states = new ArrayList<>();
                    if (loaded.customKeys == null) loaded.customKeys = new ArrayList<>();
                    instance = loaded;
                }
            } catch (Exception e) {
                System.err.println("[KeybindHelper] Không đọc được panel config, dùng mặc định: " + e);
            }
        }
        save();
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(path(), StandardCharsets.UTF_8)) {
            GSON.toJson(instance, w);
        } catch (IOException e) {
            System.err.println("[KeybindHelper] Không ghi được panel config: " + e);
        }
    }
          }
