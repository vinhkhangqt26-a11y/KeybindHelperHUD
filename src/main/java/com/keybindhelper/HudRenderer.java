package com.keybindhelper;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.ArrayList;
import java.util.List;

public final class HudRenderer {
    private HudRenderer() {}

    // Màu dạng ARGB (alpha phải là FF, nếu không chữ sẽ vô hình ở 1.21.6+)
    private static final int BG = 0x99000000;           // đen, 60% opacity
    private static final int OUTLINE = 0xFF000000;
    private static final int KEY_COLOR = 0xFF55FFFF;    // cyan
    private static final int DESC_COLOR = 0xFFEEEEEE;   // trắng xám
    private static final int HEADER_COLOR = 0xFFFFD83D; // vàng
    private static final int TITLE_COLOR = 0xFFFFFFFF;
    private static final int LINE_COLOR = 0x66FFFFFF;

    private static final int PADDING = 5;
    private static final int LINE_H = 11;
    private static final int SEP_H = 5;
    private static final int GAP = 6;

    private record Entry(String key, String desc) {}

    private static final Entry[] INVENTORY = {
            new Entry("Q", "Vứt 1 vật phẩm"),
            new Entry("Ctrl + Q", "Vứt toàn bộ 1 stack"),
            new Entry("F", "Đổi tay chính / tay phụ"),
            new Entry("Shift + Chuột trái", "Chuyển nhanh vật phẩm"),
            new Entry("1 - 9", "Chọn ô Hotbar tương ứng"),
            new Entry("Chuột giữa", "Lấy khối đang nhìn (Pick Block)"),
    };

    private static final Entry[] MOVEMENT = {
            new Entry("Giữ Shift", "Đi rón rén (không rơi mép khối)"),
            new Entry("Giữ Ctrl", "Chạy nhanh (Sprint)"),
    };

    private static final Entry[] DEBUG = {
            new Entry("F3", "Màn hình Debug"),
            new Entry("F3 + H", "Hiện độ bền & ID vật phẩm"),
            new Entry("F3 + G", "Hiện viền Chunk"),
            new Entry("F3 + B", "Hiện Hitbox thực thể"),
            new Entry("F3 + A", "Tải lại toàn bộ Chunk"),
            new Entry("F3 + ESC", "Tạm dừng, không mở menu"),
            new Entry("F3 + F4", "Bảng đổi chế độ chơi"),
    };

    private static final Entry[] EXTRA = {
            new Entry("E", "Mở kho đồ"),
            new Entry("Tab", "Danh sách người chơi"),
            new Entry("T", "Mở khung chat"),
            new Entry("F1", "Ẩn/hiện giao diện"),
            new Entry("F2", "Chụp màn hình"),
            new Entry("F5", "Đổi góc nhìn"),
            new Entry("F11", "Toàn màn hình"),
    };

    /** Một dòng của bảng: type 0 = tiêu đề nhóm, 1 = phím, 2 = đường kẻ. */
    private record Row(int type, String left, String right) {}

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        HelperConfig cfg = HelperConfig.get();
        if (!cfg.visible || mc.options.hudHidden || mc.player == null) {
            return;
        }

        TextRenderer font = mc.textRenderer;
        String keyName = KeybindHelperClient.toggleKey == null
                ? "H" : KeybindHelperClient.toggleKey.getBoundKeyLocalizedText().getString();
        String title = "[ BẢNG PHÍM TẮT HỖ TRỢ ] (Nhấn '" + keyName + "' để ẩn)";

        List<Row> rows = new ArrayList<>();
        rows.add(new Row(2, "", ""));
        if (cfg.showInventory) addGroup(rows, "Kho đồ & Vật phẩm", INVENTORY);
        if (cfg.showMovement) addGroup(rows, "Di chuyển & Tiện ích", MOVEMENT);
        if (cfg.showDebug) addGroup(rows, "Debug (F3)", DEBUG);
        if (cfg.showExtra) addGroup(rows, "Khác", EXTRA);

        int bulletW = font.getWidth("• ");
        int keyW = 0;
        int descW = 0;
        int headerW = 0;
        for (Row r : rows) {
            if (r.type() == 1) {
                keyW = Math.max(keyW, bulletW + font.getWidth(r.left()));
                descW = Math.max(descW, font.getWidth(": " + r.right()));
            } else if (r.type() == 0) {
                headerW = Math.max(headerW, font.getWidth(r.left()));
            }
        }
        int contentW = Math.max(font.getWidth(title), Math.max(headerW, keyW + GAP + descW));
        int panelW = contentW + PADDING * 2;

        int panelH = PADDING * 2 + LINE_H; // phần tiêu đề
        for (Row r : rows) {
            panelH += (r.type() == 2) ? SEP_H : LINE_H;
        }

        int screenW = ctx.getScaledWindowWidth();
        int x = cfg.anchorRight ? screenW - panelW - cfg.offsetX : cfg.offsetX;
        int y = cfg.offsetY;
        x = Math.max(0, x);
        y = Math.max(0, y);

        drawPanel(ctx, x, y, x + panelW, y + panelH);

        int cx = x + PADDING;
        int cy = y + PADDING;
        drawOutlined(ctx, font, title, cx, cy, TITLE_COLOR);
        cy += LINE_H;

        for (Row r : rows) {
            if (r.type() == 2) {
                ctx.fill(cx, cy + 1, x + panelW - PADDING, cy + 2, LINE_COLOR);
                cy += SEP_H;
            } else if (r.type() == 0) {
                drawOutlined(ctx, font, r.left(), cx, cy, HEADER_COLOR);
                cy += LINE_H;
            } else {
                drawOutlined(ctx, font, "• " + r.left(), cx, cy, KEY_COLOR);
                drawOutlined(ctx, font, ": " + r.right(), cx + keyW + GAP, cy, DESC_COLOR);
                cy += LINE_H;
            }
        }
    }

    private static void addGroup(List<Row> rows, String name, Entry[] entries) {
        rows.add(new Row(0, name, ""));
        for (Entry e : entries) {
            rows.add(new Row(1, e.key(), e.desc()));
        }
        rows.add(new Row(2, "", ""));
    }

    /** Nền bo góc nhẹ: 3 hình chữ nhật không chồng nhau (để không bị tối hơn ở chỗ chồng). */
    private static void drawPanel(DrawContext ctx, int x1, int y1, int x2, int y2) {
        ctx.fill(x1 + 1, y1, x2 - 1, y1 + 1, BG);
        ctx.fill(x1, y1 + 1, x2, y2 - 1, BG);
        ctx.fill(x1 + 1, y2 - 1, x2 - 1, y2, BG);
    }

    /** Chữ có viền đen xung quanh để dễ đọc trên mọi nền. */
    private static void drawOutlined(DrawContext ctx, TextRenderer font, String s, int x, int y, int color) {
        ctx.drawText(font, s, x + 1, y, OUTLINE, false);
        ctx.drawText(font, s, x - 1, y, OUTLINE, false);
        ctx.drawText(font, s, x, y + 1, OUTLINE, false);
        ctx.drawText(font, s, x, y - 1, OUTLINE, false);
        ctx.drawText(font, s, x, y, color, false);
    }
                }
