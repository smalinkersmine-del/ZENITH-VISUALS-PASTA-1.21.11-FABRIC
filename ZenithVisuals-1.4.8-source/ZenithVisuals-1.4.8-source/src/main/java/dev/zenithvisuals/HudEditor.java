package dev.zenithvisuals;

import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_437;
import net.minecraft.class_11909;
import net.minecraft.class_11908;

/** HUD editor with polished preview, drag positioning and uniform corner scaling. */
public final class HudEditor extends class_437 {
    private static final String[] IDS = ZenithVisuals.WIDGET_IDS;
    private static final String[] LABELS = {
        "Watermark / Logo", "Active Modules", "Armor Status", "Movement Info", "Active Binds", "Crosshair",
        "Clock", "Coordinates", "Direction", "Biome Info", "Dimension Info", "Ping Display", "Potion Effects",
        "Keystrokes", "CPS Counter", "Timer Indicator", "Memory Meter", "FPS Graph", "Item Counter", "Durability Alert",
        "Weather Display", "World Time", "Light Level", "Compass", "Jump Counter", "Notification Feed", "Speed", "Cooldowns", "Staff Active"
    };
    private static final int TEXT = 0xFFF1F4F8, MUTED = 0xFF91A0B2, PANEL = 0xF10D141E, EDGE = 0xFF293646;
    private int selected = -1;
    private boolean resizing;
    private int sideX, sideW, resetX, resetY;

    public HudEditor() { super(class_2561.method_43470("Редактор HUD · ZenithVisuals")); }
    private int value(String id, int part) { return ZenithVisuals.widgetValue(id, part, ZenithVisuals.defaultWidgetValue(id, part, field_22789, field_22790)); }

    @Override public void method_25394(class_332 g, int mx, int my, float delta) {
        int sw = field_22789, sh = field_22790;
        g.method_25294(0, 0, sw, sh, 0x65040A12);
        // Clean solid editing canvas; avoid hundreds of tiny grid draw calls each frame.

        rounded(g, 12, 10, Math.min(480, sw - 24), 48, 10, 0xF10D141E);
        outline(g, 12, 10, Math.min(480, sw - 24), 48, EDGE);
        rounded(g, 21, 18, 29, 29, 7, 0xFF202D3B);
        label(g, "ZV", 27, 28, ZenithVisuals.accent());
        label(g, "HUD EDITOR", 59, 19, TEXT);
        label(g, "ПЕРЕТАЩИВАЙ ЭЛЕМЕНТЫ · УГОЛОК МЕНЯЕТ МАСШТАБ ЦЕЛИКОМ", 59, 34, MUTED);

        // Preview frames use the same restrained panel palette as the ClickGUI.
        for (int i = 0; i < IDS.length; i++) {
            String id = IDS[i];
            if (!ZenithVisuals.enabled(id)) continue;
            int x = value(id, 0), y = value(id, 1), w = Math.max(24, value(id, 2)), h = Math.max(16, value(id, 3));
            boolean active = i == selected, hover = inside(mx, my, x, y, w, h);
            int border = active ? ZenithVisuals.accent() : (hover ? 0xFF9BAEC4 : 0xFF536174);
            rounded(g, x + 1, y + 2, w, h, 7, 0x66000000);
            rounded(g, x, y, w, h, 7, active ? 0xAA1B2A3C : 0x66303B4D);
            outline(g, x, y, w, h, border);
            if (w > 48 && h > 23) {
                label(g, LABELS[i].toUpperCase(), x + 8, y + 7, active ? TEXT : 0xFFD4DCE6);
                label(g, w + " × " + h, x + 8, y + h - 13, active ? ZenithVisuals.accent() : MUTED);
            }
            // One square grip communicates a proportional resize, never width/height stretching.
            rounded(g, x + w - 11, y + h - 11, 8, 8, 2, border);
            if (active) {
                g.method_25294(x + 4, y + 2, x + w - 4, y + 3, ZenithVisuals.accent());
            }
        }

        sideW = Math.min(214, Math.max(178, sw / 4)); sideX = sw - sideW - 12;
        rounded(g, sideX, 70, sideW, 188, 9, PANEL); outline(g, sideX, 70, sideW, 188, EDGE);
        label(g, "РАСПОЛОЖЕНИЕ", sideX + 13, 83, MUTED);
        if (selected >= 0) {
            String id = IDS[selected];
            label(g, LABELS[selected], sideX + 13, 105, TEXT);
            label(g, "X   " + value(id, 0) + "      Y   " + value(id, 1), sideX + 13, 128, ZenithVisuals.accent());
            int baseW = Math.max(1, ZenithVisuals.defaultWidgetValue(id, 2, sw, sh));
            int scale = Math.max(25, Math.min(250, Math.round(value(id, 2) * 100.0f / baseW)));
            label(g, "МАСШТАБ   " + scale + "%", sideX + 13, 151, 0xFFD5DDE7);
            label(g, "ШИРИНА И ВЫСОТА СВЯЗАНЫ", sideX + 13, 174, MUTED);
            label(g, "СОХРАНЕНИЕ АВТОМАТИЧЕСКОЕ", sideX + 13, 225, 0xFF75D3AD);
        } else {
            label(g, "ВЫБЕРИ ВИДЖЕТ", sideX + 13, 108, TEXT);
            label(g, "ЛКМ — перемещение", sideX + 13, 133, MUTED);
            label(g, "Уголок — увеличить целиком", sideX + 13, 151, MUTED);
            label(g, "R — сбросить расположение", sideX + 13, 169, MUTED);
        }

        rounded(g, sideX, 270, sideW, 80, 9, PANEL); outline(g, sideX, 270, sideW, 80, EDGE);
        label(g, "УПРАВЛЕНИЕ", sideX + 13, 284, MUTED);
        label(g, "ЛКМ  ·  ПЕРЕТАСКИВАТЬ", sideX + 13, 306, TEXT);
        label(g, "ЗАЖМИ УГОЛОК  ·  УВЕЛИЧИТЬ", sideX + 13, 324, SOFT());

        resetX = 16; resetY = sh - 38;
        rounded(g, resetX, resetY, 174, 24, 5, inside(mx, my, resetX, resetY, 174, 24) ? 0xFF293747 : 0xFF1B2633);
        outline(g, resetX, resetY, 174, 24, EDGE);
        label(g, "СБРОСИТЬ РАСПОЛОЖЕНИЕ", resetX + 10, resetY + 8, TEXT);
        label(g, "ESC  ·  НАЗАД В CLICK GUI", Math.max(180, sw - 220), sh - 29, MUTED);
    }

    @Override public boolean method_25402(class_11909 click, boolean doubled) {
        if (click.method_74245() != 0) return false;
        int mx = (int) click.comp_4798(), my = (int) click.comp_4799();
        if (inside(mx, my, resetX, resetY, 174, 24)) { ZenithVisuals.resetWidgets(); ZenithVisuals.saveConfig(); selected = -1; return true; }
        for (int i = IDS.length - 1; i >= 0; i--) {
            String id = IDS[i]; if (!ZenithVisuals.enabled(id)) continue;
            int x = value(id, 0), y = value(id, 1), w = value(id, 2), h = value(id, 3);
            if (inside(mx, my, x, y, w, h)) { selected = i; resizing = mx >= x + w - 15 && my >= y + h - 15; return true; }
        }
        selected = -1; resizing = false; return true;
    }

    @Override public boolean method_25403(class_11909 click, double dx, double dy) {
        if (selected < 0) return false;
        String id = IDS[selected]; int x = value(id, 0), y = value(id, 1), w = Math.max(24, value(id, 2)), h = Math.max(16, value(id, 3));
        if (resizing) {
            // Project the pointer delta onto the widget's diagonal. A single factor updates W and H.
            double factor = 1.0 + (dx + dy) / Math.max(60.0, w + h);
            double minFactor = Math.max(24.0 / w, 16.0 / h);
            double maxFactor = Math.min(560.0 / w, 320.0 / h);
            factor = Math.max(minFactor, Math.min(maxFactor, factor));
            w = Math.max(24, (int)Math.round(w * factor));
            h = Math.max(16, (int)Math.round(h * factor));
        } else {
            x = Math.max(0, Math.min(field_22789 - 18, x + (int)Math.round(dx)));
            y = Math.max(0, Math.min(field_22790 - 12, y + (int)Math.round(dy)));
        }
        ZenithVisuals.setWidget(id, x, y, w, h); return true;
    }

    @Override public boolean method_25406(class_11909 click) { resizing = false; ZenithVisuals.saveConfig(); return true; }
    @Override public boolean method_25404(class_11908 key) {
        int code = key.comp_4795();
        if (code == 256) { ZenithVisuals.saveConfig(); try { field_22787.method_1507(new ClickGui()); } catch (Throwable ignored) {} return true; }
        if (code == 82) { ZenithVisuals.resetWidgets(); ZenithVisuals.saveConfig(); selected = -1; return true; }
        return false;
    }
    @Override public boolean method_25421() { return false; }

    private void rounded(class_332 g, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (r <= 0) { g.method_25294(x, y, x + w, y + h, color); return; }
        int baseAlpha = (color >>> 24) & 255;
        for (int row = 0; row < h; row++) {
            int cornerRow = row < r ? row : (row >= h - r ? h - 1 - row : -1);
            double inset = 0.0;
            if (cornerRow >= 0) {
                double dy = r - cornerRow - 0.5;
                inset = r - Math.sqrt(Math.max(0.0, r * (double) r - dy * dy));
            }
            int leftFull = (int) Math.ceil(inset - 1.0E-9);
            int rightFull = (int) Math.floor(w - inset + 1.0E-9);
            double leftCoverage = Math.max(0.0, Math.min(1.0, leftFull - inset));
            double rightCoverage = Math.max(0.0, Math.min(1.0, (w - inset) - rightFull));
            if (leftCoverage > 0.001 && leftFull > 0) {
                int a = (int) Math.round(baseAlpha * leftCoverage);
                g.method_25294(x + leftFull - 1, y + row, x + leftFull, y + row + 1, (color & 0x00FFFFFF) | (a << 24));
            }
            if (rightFull > leftFull) g.method_25294(x + leftFull, y + row, x + rightFull, y + row + 1, color);
            if (rightCoverage > 0.001 && rightFull < w) {
                int a = (int) Math.round(baseAlpha * rightCoverage);
                g.method_25294(x + rightFull, y + row, x + rightFull + 1, y + row + 1, (color & 0x00FFFFFF) | (a << 24));
            }
        }
    }
    private int roundedInset(int radius, int row) {
        double dy = radius - row - 0.5;
        return Math.max(0, Math.min(radius, (int)Math.round(radius - Math.sqrt(Math.max(0.0, radius * radius - dy * dy)))));
    }
    private void outline(class_332 g, int x, int y, int w, int h, int c) {
        int r = Math.max(0, Math.min(7, Math.min(w, h) / 2));
        for (int row = 0; row < h; row++) {
            int inset = 0;
            if (row < r) inset = roundedInset(r, row);
            else if (row >= h - r) inset = roundedInset(r, h - 1 - row);
            if (row == 0 || row == h - 1) g.method_25294(x + inset, y + row, x + w - inset, y + row + 1, c);
            else {
                g.method_25294(x + inset, y + row, x + inset + 1, y + row + 1, c);
                g.method_25294(x + w - inset - 1, y + row, x + w - inset, y + row + 1, c);
            }
        }
    }
    private void label(class_332 g, String value, int x, int y, int color) { class_2561 text = (class_2561)TextFonts.apply(class_2561.method_43470(value), ZenithVisuals.fontIndex, true); g.method_27535(field_22793, text, x, y, color); }
    private boolean inside(int mx, int my, int x, int y, int w, int h) { return mx >= x && mx <= x + w && my >= y && my <= y + h; }
    private int SOFT() { return 0xFFCAD3DE; }
}
