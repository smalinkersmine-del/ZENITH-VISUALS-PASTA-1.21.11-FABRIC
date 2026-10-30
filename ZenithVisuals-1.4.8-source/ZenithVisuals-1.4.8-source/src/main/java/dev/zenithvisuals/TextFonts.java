package dev.zenithvisuals;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.function.UnaryOperator;

/**
 * Safe Minecraft 1.21.11 text font selector.
 * Uses only public intermediary interfaces and keeps the default font as a fallback.
 */
final class TextFonts {
    private TextFonts() {}

    static Object apply(Object text, int fontIndex, boolean bold) {
        if (text == null) return null;
        try {
            Class<?> identifierType = Class.forName("net.minecraft.class_2960");
            Object id = identifierType.getMethod("method_60655", String.class, String.class)
                    .invoke(null, "minecraft", fontId(fontIndex));

            Class<?> fontSourceType = Class.forName("net.minecraft.class_11719");
            Class<?> fontRecordType = Class.forName("net.minecraft.class_11719$class_11721");
            Constructor<?> constructor = fontRecordType.getConstructor(identifierType);
            Object source = constructor.newInstance(id);

            Class<?> styleType = Class.forName("net.minecraft.class_2583");
            Method withFont = styleType.getMethod("method_27704", fontSourceType);
            Method withBold = styleType.getMethod("method_10982", Boolean.class);
            Method withItalic = styleType.getMethod("method_10978", Boolean.class);

            final int selected = Math.floorMod(fontIndex, 3);
            UnaryOperator<Object> updater = style -> {
                try {
                    Object result = withFont.invoke(style, source);
                    result = withBold.invoke(result, bold || selected == 2);
                    if (selected == 2) result = withItalic.invoke(result, Boolean.TRUE);
                    return result;
                } catch (ReflectiveOperationException error) {
                    throw new IllegalStateException("Cannot apply ZenithVisuals font", error);
                }
            };
            Class<?> mutableTextType = Class.forName("net.minecraft.class_5250");
            mutableTextType.getMethod("method_27694", UnaryOperator.class).invoke(text, updater);
        } catch (Throwable error) {
            // Keep Minecraft's default font if the selected resource is unavailable.
            if (Boolean.getBoolean("zenithvisuals.debugFonts")) {
                Throwable root=error;
                while(root instanceof java.lang.reflect.InvocationTargetException && ((java.lang.reflect.InvocationTargetException)root).getCause()!=null) root=((java.lang.reflect.InvocationTargetException)root).getCause();
                System.err.println("[ZenithVisuals] Font fallback: " + root.getClass().getSimpleName() + ": " + root.getMessage());
                root.printStackTrace();
            }
        }
        return text;
    }

    private static String fontId(int index) {
        return switch (Math.floorMod(index, 3)) {
            case 1 -> "uniform";
            case 2 -> "default";
            default -> "default";
        };
    }
}
