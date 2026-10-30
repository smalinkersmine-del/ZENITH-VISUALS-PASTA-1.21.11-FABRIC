package dev.zenithvisuals;

import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_332;
import net.minecraft.class_437;
import net.minecraft.class_11909;
import net.minecraft.class_11908;
import java.util.*;

/** Five-column compact ClickGUI matching the supplied in-game reference. */
public final class ClickGui extends class_437 {
    private static final String[] CATEGORIES = {"Combat", "Movement", "Player", "Misc", "Visuals"};
    private static final String[][] ITEMS = {
        {"target_esp", "crosshair", "cps_counter", "ping_display"},
        {"movement_info", "speed", "coordinates", "direction", "compass", "keystrokes", "jump_counter"},
        {"armor_status", "potion_status", "cooldowns", "item_counter", "durability_alert", "biome_info", "dimension_info", "light_level"},
        {"clock", "session_timer", "memory_meter", "fps_graph", "weather_display", "world_time", "notification_feed", "discord_presence", "fps_optimizer"},
        {"watermark", "active_modules", "keybinds", "staff_active", "fullbright"}
    };

    private static final int TEXT = 0xFFF1F3F7;
    private static final int MUTED = 0xFF9AA6B5;
    private static final int EDGE = 0x4A8B9AAF;
    private static final int COL_BG = 0xB70A0D13;
    private static final int COL_HEAD = 0xD0121822;
    private static final int ROW_HOVER = 0x744D5D70;
    private static final int ROW_ACTIVE = 0x704D6D88;
    private static final int ROW_H = 22;
    private static final int COL_GAP = 8;

    private int[] colX = new int[5];
    private int colW, colY, headerX, headerY, headerW;
    private int hudX, themeX, fontX, bindX, closeX;
    private String selectedFeatureId;
    private String bindingFeature;
    private boolean bindingMode;
    private int settingsX, settingsY, settingsW = 210, settingsH;

    public ClickGui() { super(class_2561.method_43470("ZenithVisuals")); }

    private void layout() {
        int sw = field_22789;
        int available = Math.min(1150, Math.max(400, (int)Math.round(sw * 0.74)));
        colW = Math.max(88, (available - COL_GAP * 4) / 5);
        int total = colW * 5 + COL_GAP * 4;
        int start = (sw - total) / 2;
        for (int i = 0; i < 5; i++) colX[i] = start + i * (colW + COL_GAP);
        colY = Math.max(64, (int)Math.round(field_22790 * 0.26));
        headerW = Math.min(500, Math.max(340, sw - 32));
        headerX = (sw - headerW) / 2;
        headerY = Math.max(4, (int)Math.round(field_22790 * 0.02));
        int right = headerX + headerW - 10;
        closeX = right - 25;
        bindX = closeX - 51;
        fontX = bindX - 49;
        themeX = fontX - 58;
        hudX = themeX - 48;
    }

    @Override
    public void method_25394(class_332 g, int mouseX, int mouseY, float delta) {
        layout();
        // Keep the world visible, as in the supplied reference. Only the panels are translucent.
        rounded(g, headerX + 2, headerY + 2, headerW, 30, 7, 0x55000000);
        rounded(g, headerX, headerY, headerW, 30, 7, 0xB70B1018);
        outline(g, headerX, headerY, headerW, 30, 7, EDGE);
        drawLogo(g, headerX + 8, headerY + 5, 20, 20);
        draw(g, "ZENITH", headerX + 34, headerY + 6, TEXT, true);
        draw(g, "VISUALS", headerX + 34, headerY + 17, ZenithVisuals.accent(), true);
        topButton(g, hudX, "HUD", 42, mouseX, mouseY);
        topButton(g, themeX, "THEME", 52, mouseX, mouseY);
        topButton(g, fontX, "FONT", 44, mouseX, mouseY);
        topButton(g, bindX, "BIND", 46, mouseX, mouseY);
        topButton(g, closeX, "×", 22, mouseX, mouseY);
        gradientStrip(g, headerX + 1, headerY + 29, headerW - 2, 1);

        for (int i = 0; i < CATEGORIES.length; i++) renderColumn(g, i, mouseX, mouseY);
        if (selectedFeatureId != null) renderSettings(g, mouseX, mouseY);
        if (bindingFeature != null) {
            int w = Math.min(330, field_22789 - 24), x = (field_22789 - w) / 2, y = field_22790 - 48;
            rounded(g, x, y, w, 30, 7, 0xE50C121B); outline(g, x, y, w, 30, 7, EDGE);
            draw(g, "НАЖМИ КЛАВИШУ ДЛЯ " + bindingFeature.toUpperCase(Locale.ROOT) + " · ESC — ОТМЕНА", x + 10, y + 11, ZenithVisuals.accent(), true);
        }
    }

    private void renderColumn(class_332 g, int index, int mx, int my) {
        String[] ids = ITEMS[index];
        int x = colX[index], y = colY;
        int h = 28 + ids.length * ROW_H + 7;
        rounded(g, x + 2, y + 3, colW, h, 7, 0x55000000);
        rounded(g, x, y, colW, h, 7, COL_BG);
        outline(g, x, y, colW, h, 7, EDGE);
        rounded(g, x + 1, y + 1, colW - 2, 25, 6, COL_HEAD);
        draw(g, CATEGORIES[index], x + 9, y + 9, 0xFFF4F6FA, true);
        draw(g, String.format(Locale.ROOT, "%02d", ids.length), x + colW - 22, y + 9, 0xFF78879A, false);
        fill(g, x + 7, y + 27, x + colW - 7, y + 28, 0x553F4D5E);

        for (int i = 0; i < ids.length; i++) {
            String id = ids[i];
            ZenithVisuals.Feature f = ZenithVisuals.feature(id);
            if (f == null) continue;
            int ry = y + 31 + i * ROW_H;
            boolean hover = inside(mx, my, x + 4, ry, colW - 8, ROW_H - 1);
            boolean selected = id.equals(selectedFeatureId);
            if (selected) rounded(g, x + 4, ry, colW - 8, ROW_H - 1, 4, ROW_ACTIVE);
            else if (hover) rounded(g, x + 4, ry, colW - 8, ROW_H - 1, 4, ROW_HOVER);
            if (f.enabled) fill(g, x + 5, ry + 5, x + 7, ry + 16, ZenithVisuals.accent());
            String shown = shorten(f.name, Math.max(7, (colW - 29) / 6));
            draw(g, shown, x + 10, ry + 7, f.enabled ? TEXT : 0xFFB2BBC7, f.enabled);
            // Small state marker instead of bulky switches.
            if (f.enabled) rounded(g, x + colW - 15, ry + 8, 5, 5, 2, ZenithVisuals.accent());
            else rounded(g, x + colW - 15, ry + 8, 5, 5, 2, 0xFF566170);
        }
    }

    private void renderSettings(class_332 g, int mx, int my) {
        ZenithVisuals.Feature f = ZenithVisuals.feature(selectedFeatureId);
        if (f == null) return;
        int[] point = findFeaturePoint(selectedFeatureId);
        settingsW = Math.min(224, field_22789 - 20);
        settingsH = featureSettings(selectedFeatureId).length * 24 + (selectedFeatureId.equals("target_esp") ? 5 * 25 : 0) + 58;
        settingsX = point[0] + colW + 7;
        if (settingsX + settingsW > field_22789 - 8) settingsX = point[0] - settingsW - 7;
        settingsX = Math.max(8, Math.min(field_22789 - settingsW - 8, settingsX));
        settingsY = Math.max(8, Math.min(field_22790 - settingsH - 8, point[1] - 10));

        rounded(g, settingsX + 2, settingsY + 3, settingsW, settingsH, 8, 0x70000000);
        rounded(g, settingsX, settingsY, settingsW, settingsH, 8, 0xEB0B111A);
        outline(g, settingsX, settingsY, settingsW, settingsH, 8, EDGE);
        draw(g, f.name.toUpperCase(Locale.ROOT), settingsX + 11, settingsY + 11, TEXT, true);
        draw(g, "НАСТРОЙКИ ФУНКЦИИ", settingsX + 11, settingsY + 24, MUTED, false);
        int y = settingsY + 39;
        for (String key : featureSettings(selectedFeatureId)) {
            boolean value = ZenithVisuals.setting(key);
            rounded(g, settingsX + 7, y, settingsW - 14, 21, 4, inside(mx, my, settingsX + 7, y, settingsW - 14, 21) ? ROW_HOVER : 0x4C263342);
            draw(g, settingLabel(key), settingsX + 12, y + 7, 0xFFE1E6ED, false);
            drawToggle(g, settingsX + settingsW - 34, y + 5, value);
            y += 24;
        }
        if (selectedFeatureId.equals("target_esp")) {
            settingCycle(g, y, "РЕЖИМ", ZenithVisuals.ESP_MODES[Math.floorMod(ZenithVisuals.targetEspMode, ZenithVisuals.ESP_MODES.length)], mx, my); y += 25;
            settingCycle(g, y, "РАЗМЕР", ZenithVisuals.targetEspSize + "%", mx, my); y += 25;
            settingCycle(g, y, "СКОРОСТЬ", Integer.toString(ZenithVisuals.targetEspSpeed + 1), mx, my); y += 25;
            settingCycle(g, y, "ПРОЗРАЧНОСТЬ", Integer.toString(ZenithVisuals.targetEspOpacity), mx, my); y += 25;
            settingCycle(g, y, "ВРЕМЯ ЦЕЛИ", ZenithVisuals.targetEspDisplayTime + " СЕК", mx, my);
        }
        draw(g, "ПКМ СНАРУЖИ — ЗАКРЫТЬ", settingsX + 11, settingsY + settingsH - 15, 0xFF687789, false);
    }

    private int[] findFeaturePoint(String id) {
        for (int c = 0; c < ITEMS.length; c++) for (int i = 0; i < ITEMS[c].length; i++) if (ITEMS[c][i].equals(id)) return new int[]{colX[c], colY + 31 + i * ROW_H};
        return new int[]{field_22789 / 2, colY};
    }

    private static String[] featureSettings(String id) {
        return switch (id) {
            case "target_esp" -> new String[]{"target_esp.box", "target_esp.ring", "target_esp.onlyVisible", "target_esp.hurtColor", "target_esp.throughWalls"};
            case "fullbright" -> new String[0];
            case "watermark" -> new String[]{"watermark.fps"};
            case "active_modules" -> new String[]{"active.showKeys", "active.background"};
            case "armor_status" -> new String[]{"armor.durability", "armor.background"};
            case "movement_info" -> new String[]{"movement.coords", "movement.speed", "movement.facing"};
            case "keybinds" -> new String[]{"keybinds.compact"};
            case "cooldowns", "staff_active", "speed", "session_timer" -> new String[]{"widget.background"};
            case "crosshair" -> new String[]{"crosshair.outline", "crosshair.dot"};
            case "potion_status" -> new String[]{"widget.background", "potion.showDuration"};
            case "item_counter" -> new String[]{"widget.background", "item.showCount"};
            case "durability_alert" -> new String[]{"widget.background", "durability.onlyLow"};
            default -> new String[]{"widget.background"};
        };
    }
    private static String settingLabel(String key) {
        return switch (key) {
            case "target_esp.box" -> "3D-рамка"; case "target_esp.ring" -> "Анимированные кольца";
            case "target_esp.onlyVisible" -> "Только видимые"; case "target_esp.hurtColor" -> "Цвет при уроне";
            case "target_esp.throughWalls" -> "Сквозь стены"; case "watermark.fps" -> "Показывать FPS";
            case "active.showKeys" -> "Показывать клавиши"; case "active.background" -> "Фон списка";
            case "armor.durability" -> "Прочность брони"; case "armor.background" -> "Фон брони";
            case "movement.coords" -> "Координаты"; case "movement.speed" -> "Скорость";
            case "movement.facing" -> "Направление"; case "keybinds.compact" -> "Компактный режим";
            case "crosshair.outline" -> "Обводка"; case "crosshair.dot" -> "Центральная точка";
            case "widget.background" -> "Фон виджета";
            case "potion.showDuration" -> "Время действия"; case "item.showCount" -> "Количество";
            case "durability.onlyLow" -> "Только низкая прочность"; default -> key;
        };
    }

    private void settingCycle(class_332 g, int y, String label, String value, int mx, int my) {
        int x = settingsX + 7, w = settingsW - 14;
        rounded(g, x, y, w, 21, 4, inside(mx, my, x, y, w, 21) ? ROW_HOVER : 0x4C263342);
        draw(g, label, x + 6, y + 7, MUTED, true);
        draw(g, value, x + w - Math.max(60, value.length() * 6 + 10), y + 7, ZenithVisuals.accent(), true);
    }

    @Override public boolean method_25402(class_11909 click, boolean doubled) {
        int button = click.method_74245();
        if (button != 0 && button != 1) return false;
        int mx = (int) click.comp_4798(), my = (int) click.comp_4799();
        layout();
        if (inside(mx, my, hudX, headerY + 3, 42, 24)) { try { field_22787.method_1507(new HudEditor()); } catch (Throwable ignored) {} return true; }
        if (inside(mx, my, themeX, headerY + 3, 52, 24)) { ZenithVisuals.themeIndex = (ZenithVisuals.themeIndex + 1) % ZenithVisuals.THEMES.length; ZenithVisuals.saveConfig(); return true; }
        if (inside(mx, my, fontX, headerY + 3, 44, 24)) { ZenithVisuals.fontIndex = (ZenithVisuals.fontIndex + 1) % ZenithVisuals.FONT_NAMES.length; ZenithVisuals.saveConfig(); return true; }
        if (inside(mx, my, bindX, headerY + 3, 46, 24)) { bindingMode = !bindingMode; selectedFeatureId = null; return true; }
        if (inside(mx, my, closeX, headerY + 3, 22, 24)) { close(); return true; }

        if (selectedFeatureId != null && inside(mx, my, settingsX, settingsY, settingsW, settingsH)) {
            int y = settingsY + 39;
            for (String key : featureSettings(selectedFeatureId)) {
                if (inside(mx, my, settingsX + 7, y, settingsW - 14, 21)) { ZenithVisuals.toggleSetting(key); ZenithVisuals.saveConfig(); return true; }
                y += 24;
            }
            if (selectedFeatureId.equals("target_esp")) {
                if (inside(mx,my,settingsX+7,y,settingsW-14,21)) { ZenithVisuals.targetEspMode=(ZenithVisuals.targetEspMode+1)%ZenithVisuals.ESP_MODES.length; ZenithVisuals.saveConfig(); return true; } y+=25;
                if (inside(mx,my,settingsX+7,y,settingsW-14,21)) { ZenithVisuals.targetEspSize=ZenithVisuals.targetEspSize>=180?60:ZenithVisuals.targetEspSize+20; ZenithVisuals.saveConfig(); return true; } y+=25;
                if (inside(mx,my,settingsX+7,y,settingsW-14,21)) { ZenithVisuals.targetEspSpeed=(ZenithVisuals.targetEspSpeed+1)%5; ZenithVisuals.saveConfig(); return true; } y+=25;
                if (inside(mx,my,settingsX+7,y,settingsW-14,21)) { ZenithVisuals.targetEspOpacity=ZenithVisuals.targetEspOpacity>=245?90:ZenithVisuals.targetEspOpacity+35; ZenithVisuals.saveConfig(); return true; } y+=25;
                if (inside(mx,my,settingsX+7,y,settingsW-14,21)) { ZenithVisuals.targetEspDisplayTime=ZenithVisuals.targetEspDisplayTime>=10?1:ZenithVisuals.targetEspDisplayTime+1; ZenithVisuals.saveConfig(); return true; }
            }
            return true;
        }
        if (selectedFeatureId != null && button == 1) { selectedFeatureId = null; return true; }

        for (int c = 0; c < ITEMS.length; c++) {
            String[] ids = ITEMS[c];
            for (int i = 0; i < ids.length; i++) {
                int x = colX[c] + 4, y = colY + 31 + i * ROW_H;
                if (!inside(mx, my, x, y, colW - 8, ROW_H - 1)) continue;
                ZenithVisuals.Feature f = ZenithVisuals.feature(ids[i]);
                if (f == null) return true;
                if (bindingMode) { bindingFeature = f.id; return true; }
                if (button == 1) selectedFeatureId = f.id;
                else { f.enabled = !f.enabled; ZenithVisuals.notify((f.enabled ? "Включено: " : "Выключено: ") + f.name); if (f.id.equals("discord_presence") && f.enabled) DiscordRpc.startIfConfigured(); ZenithVisuals.saveConfig(); }
                return true;
            }
        }
        if (button == 1 && selectedFeatureId != null) selectedFeatureId = null;
        return true;
    }

    @Override public boolean method_25404(class_11908 key) {
        int code = key.comp_4795();
        if (bindingFeature != null) {
            if (code == 256) { bindingFeature = null; return true; }
            ZenithVisuals.Feature f = ZenithVisuals.feature(bindingFeature);
            if (f != null) { f.key = code; ZenithVisuals.saveConfig(); }
            bindingFeature = null; bindingMode = false; return true;
        }
        if (code == 256 || code == 344) { close(); return true; }
        return false;
    }

    @Override public boolean method_25421() { return false; }
    private void close() { ZenithVisuals.menuOpen = false; ZenithVisuals.saveConfig(); try { field_22787.method_1507(null); } catch (Throwable ignored) {} }
    private void topButton(class_332 g, int x, String s, int w, int mx, int my) {
        boolean hover = inside(mx, my, x, headerY + 3, w, 24);
        rounded(g, x, headerY + 3, w, 24, 5, hover ? 0xCC293748 : 0x77313C4B);
        draw(g, s, x + (w - s.length() * 6) / 2, headerY + 12, hover ? TEXT : MUTED, true);
    }
    private void drawToggle(class_332 g, int x, int y, boolean active) {
        rounded(g, x, y, 20, 11, 6, active ? ZenithVisuals.accent() : 0xFF3B4655);
        rounded(g, x + (active ? 10 : 2), y + 2, 7, 7, 4, 0xFFF6F7F8);
    }
    private void drawLogo(class_332 g, int x, int y, int w, int h) {
        try { class_2960 id = class_2960.method_60655("zenithvisuals", "textures/gui/zenith-logo.png"); g.getClass().getMethod("method_70845", class_2960.class, int.class, int.class, int.class, int.class, float.class, float.class, float.class, float.class).invoke(g, id, x, y, x + w, y + h, 0f, 1f, 0f, 1f); }
        catch (Throwable ignored) { draw(g, "ZV", x + 3, y + 7, ZenithVisuals.accent(), true); }
    }
    private void gradientStrip(class_332 g, int x, int y, int w, int h) {
        // Static accent rail: one draw call, no animated/RGB segments.
        if (w > 0 && h > 0) fill(g, x, y, x + w, y + h, ZenithVisuals.accent());
    }
    private void outline(class_332 g, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int row = 0; row < h; row++) {
            int inset = 0;
            if (row < r) inset = roundedInset(r, row);
            else if (row >= h - r) inset = roundedInset(r, h - 1 - row);
            if (row == 0 || row == h - 1) fill(g, x + inset, y + row, x + w - inset, y + row + 1, color);
            else {
                fill(g, x + inset, y + row, x + inset + 1, y + row + 1, color);
                fill(g, x + w - inset - 1, y + row, x + w - inset, y + row + 1, color);
            }
        }
    }
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
    private void fill(class_332 g,int x1,int y1,int x2,int y2,int color){g.method_25294(x1,y1,x2,y2,color);}
    private void draw(class_332 g,String s,int x,int y,int color,boolean bold){class_2561 t=(class_2561)TextFonts.apply(class_2561.method_43470(s),ZenithVisuals.fontIndex,bold);g.method_27535(field_22793,t,x,y,color);}
    private boolean inside(int mx,int my,int x,int y,int w,int h){return mx>=x&&mx<=x+w&&my>=y&&my<=y+h;}
    private String shorten(String s,int max){return s.length()<=max?s:s.substring(0,Math.max(1,max-1))+"…";}
}
