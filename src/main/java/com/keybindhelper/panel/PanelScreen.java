package com.keybindhelper.panel;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Bảng chức năng, có 3 chế độ:
 *  - USE : các nút đang bật hiện ra, chạm vào để chạy chức năng.
 *  - EDIT: bật/tắt từng nút, thêm/xóa phím tự tạo.
 *  - MOVE: đổi vị trí nút bằng các mũi tên.
 */
public class PanelScreen extends Screen {
    private enum Mode { USE, EDIT, MOVE }

    private static final int WHITE = 0xFFFFFFFF;
    private static final int YELLOW = 0xFFFFD83D;

    private Mode mode = Mode.USE;
    private int page = 0;
    private int step = 5;
    private String selectedId = null;
    private String nameInput = "";
    private String keyInput = "";
    private String message = "";

    // Vị trí các dòng chữ phụ trong chế độ EDIT (tính lại mỗi lần init)
    private int editLeft;
    private int pagingY;
    private int addRowY;
    private int pageCount = 1;

    public PanelScreen() {
        super(Text.literal("Bảng chức năng"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void init() {
        switch (mode) {
            case USE -> initUse();
            case EDIT -> initEdit();
            default -> initMove();
        }
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private int buttonWidth(String label) {
        return textRenderer.getWidth(label) + 12;
    }

    private void switchMode(Mode m) {
        mode = m;
        message = "";
        clearAndInit();
    }

    // ---------------------------------------------------------------- USE

    private void initUse() {
        PanelConfig cfg = PanelConfig.get();
        for (PanelActions.ActionDef a : PanelActions.all()) {
            PanelConfig.ButtonState st = cfg.state(a.id());
            if (!st.shown) {
                continue;
            }
            int w = buttonWidth(a.label());
            int x = clamp(st.x, 0, Math.max(0, width - w));
            int y = clamp(st.y, 0, Math.max(0, height - 20));
            addDrawableChild(ButtonWidget.builder(Text.literal(a.label()), b -> press(a))
                    .dimensions(x, y, w, 20).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Đóng"), b -> dismiss())
                .dimensions(4, height - 22, 48, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Chỉnh sửa"), b -> switchMode(Mode.EDIT))
                .dimensions(width - 72, height - 22, 68, 18).build());
    }

    private void dismiss() {
        if (client != null) {
            client.setScreen(null);
        }
    }

    private void press(PanelActions.ActionDef a) {
        if (client == null) {
            return;
        }
        if (PanelConfig.get().closeAfterTap) {
            client.setScreen(null);
        }
        try {
            a.run().accept(client);
        } catch (RuntimeException e) {
            System.err.println("[KeybindHelper] Lỗi khi chạy chức năng " + a.id() + ": " + e);
        }
    }

    // ---------------------------------------------------------------- EDIT

    private void initEdit() {
        PanelConfig cfg = PanelConfig.get();
        List<PanelActions.ActionDef> list = PanelActions.all();

        int rowsPerPage = Math.max(2, Math.min(8, (height - 106) / 22));
        pageCount = Math.max(1, (list.size() + rowsPerPage - 1) / rowsPerPage);
        page = clamp(page, 0, pageCount - 1);

        editLeft = Math.max(4, width / 2 - 118);
        int y = 20;
        int from = page * rowsPerPage;
        int to = Math.min(list.size(), from + rowsPerPage);
        for (int i = from; i < to; i++) {
            PanelActions.ActionDef a = list.get(i);
            PanelConfig.ButtonState st = cfg.state(a.id());
            String mark = st.shown ? "[ON]  " : "[OFF] ";
            addDrawableChild(ButtonWidget.builder(Text.literal(mark + a.label()), b -> {
                st.shown = !st.shown;
                PanelConfig.save();
                clearAndInit();
            }).dimensions(editLeft, y, 170, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Vị trí"), b -> {
                st.shown = true;
                PanelConfig.save();
                selectedId = a.id();
                switchMode(Mode.MOVE);
            }).dimensions(editLeft + 172, y, 44, 20).build());
            if (a.custom()) {
                addDrawableChild(ButtonWidget.builder(Text.literal("X"), b -> {
                    PanelConfig.get().removeCustom(a.id());
                    PanelConfig.save();
                    clearAndInit();
                }).dimensions(editLeft + 218, y, 20, 20).build());
            }
            y += 22;
        }

        pagingY = 20 + rowsPerPage * 22 + 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
            page = Math.max(0, page - 1);
            clearAndInit();
        }).dimensions(editLeft, pagingY, 24, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
            page = Math.min(pageCount - 1, page + 1);
            clearAndInit();
        }).dimensions(editLeft + 146, pagingY, 24, 18).build());

        addRowY = pagingY + 24;
        TextFieldWidget nameField = new TextFieldWidget(textRenderer, editLeft + 26, addRowY, 76, 18, Text.literal("Tên"));
        nameField.setMaxLength(12);
        nameField.setText(nameInput);
        nameField.setChangedListener(s -> nameInput = s);
        addDrawableChild(nameField);

        TextFieldWidget keyField = new TextFieldWidget(textRenderer, editLeft + 138, addRowY, 50, 18, Text.literal("Phím"));
        keyField.setMaxLength(20);
        keyField.setText(keyInput);
        keyField.setChangedListener(s -> keyInput = s);
        addDrawableChild(keyField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Thêm"), b -> addCustomFromInput())
                .dimensions(editLeft + 192, addRowY - 1, 46, 20).build());

        String toggleLabel = "Đóng sau khi bấm: " + (cfg.closeAfterTap ? "Bật" : "Tắt");
        int toggleW = buttonWidth(toggleLabel);
        addDrawableChild(ButtonWidget.builder(Text.literal("Xong"), b -> switchMode(Mode.USE))
                .dimensions(4, height - 24, 56, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(toggleLabel), b -> {
            PanelConfig c = PanelConfig.get();
            c.closeAfterTap = !c.closeAfterTap;
            PanelConfig.save();
            clearAndInit();
        }).dimensions(width - toggleW - 4, height - 24, toggleW, 20).build());
    }

    private void addCustomFromInput() {
        String key = keyInput.trim();
        if (key.isEmpty() || PanelActions.resolveKey(key) == null) {
            message = "Phím không hợp lệ (ví dụ: r, g, f5, space, shift, tab, lmb).";
            return;
        }
        String name = nameInput.trim();
        if (name.isEmpty()) {
            name = key.toUpperCase();
        }
        PanelConfig.get().addCustom(name, key);
        PanelConfig.save();
        nameInput = "";
        keyInput = "";
        message = "Đã thêm phím \"" + name + "\".";
        page = Integer.MAX_VALUE; // sẽ được kẹp về trang cuối trong initEdit()
        clearAndInit();
    }

    // ---------------------------------------------------------------- MOVE

    private void initMove() {
        PanelConfig cfg = PanelConfig.get();
        for (PanelActions.ActionDef a : PanelActions.all()) {
            PanelConfig.ButtonState st = cfg.state(a.id());
            if (!st.shown) {
                continue;
            }
            String label = a.id().equals(selectedId) ? "> " + a.label() : a.label();
            int w = buttonWidth(label);
            int x = clamp(st.x, 0, Math.max(0, width - w));
            int y = clamp(st.y, 0, Math.max(0, height - 20));
            addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> {
                selectedId = a.id();
                clearAndInit();
            }).dimensions(x, y, w, 20).build());
        }

        int by = height - 24;
        int bx = Math.max(2, width / 2 - 118);
        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> nudge(-1, 0)).dimensions(bx, by, 22, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> nudge(1, 0)).dimensions(bx + 24, by, 22, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("^"), b -> nudge(0, -1)).dimensions(bx + 48, by, 22, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("v"), b -> nudge(0, 1)).dimensions(bx + 72, by, 22, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Bước: " + step), b -> {
            step = (step == 1) ? 5 : (step == 5 ? 10 : 1);
            clearAndInit();
        }).dimensions(bx + 98, by, 64, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Xong"), b -> switchMode(Mode.EDIT))
                .dimensions(bx + 166, by, 52, 20).build());
    }

    private void nudge(int dx, int dy) {
        if (selectedId == null) {
            return;
        }
        PanelActions.ActionDef a = PanelActions.find(selectedId);
        if (a == null) {
            return;
        }
        PanelConfig.ButtonState st = PanelConfig.get().state(selectedId);
        int w = buttonWidth("> " + a.label());
        st.x = clamp(st.x + dx * step, 0, Math.max(0, width - w));
        st.y = clamp(st.y + dy * step, 0, Math.max(0, height - 20));
        PanelConfig.save();
        clearAndInit();
    }

    // ---------------------------------------------------------------- RENDER

    private void centered(DrawContext ctx, String s, int y, int color) {
        ctx.drawText(textRenderer, s, width / 2 - textRenderer.getWidth(s) / 2, y, color, true);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        switch (mode) {
            case USE -> {
                boolean any = false;
                for (PanelActions.ActionDef a : PanelActions.all()) {
                    if (PanelConfig.get().state(a.id()).shown) {
                        any = true;
                        break;
                    }
                }
                centered(context, any ? "Chạm nút để dùng" : "Chưa có nút nào - bấm \"Chỉnh sửa\" để bật nút", 6, WHITE);
            }
            case EDIT -> {
                centered(context, "Chỉnh sửa: bấm tên để Bật/Tắt nút", 6, WHITE);
                context.drawText(textRenderer, "Trang " + (page + 1) + "/" + pageCount,
                        editLeft + 74, pagingY + 5, WHITE, true);
                context.drawText(textRenderer, "Tên", editLeft, addRowY + 5, WHITE, true);
                context.drawText(textRenderer, "Phím", editLeft + 106, addRowY + 5, WHITE, true);
                if (!message.isEmpty()) {
                    context.drawText(textRenderer, message, editLeft, addRowY + 24, YELLOW, true);
                }
            }
            default -> {
                centered(context, "Chạm vào một nút để chọn, rồi dùng mũi tên để dời", 6, WHITE);
                if (selectedId != null) {
                    PanelConfig.ButtonState st = PanelConfig.get().state(selectedId);
                    centered(context, "X: " + st.x + "   Y: " + st.y, height - 38, YELLOW);
                }
            }
        }
    }
                               }
