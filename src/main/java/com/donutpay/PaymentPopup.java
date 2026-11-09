package com.donutpay;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The big flashy popup for payments of 5B+.
 * - One payment: big animated panel for ~4.5 seconds.
 * - Several payments waiting in the queue: small, quick panel at the top so they don't pile up.
 */
public final class PaymentPopup {
    private PaymentPopup() {}

    private record Alert(String name, long amount) {}

    private static final Deque<Alert> QUEUE = new ArrayDeque<>();
    private static Alert current;
    private static PlayerSkin currentSkin;
    private static long startMs;
    private static boolean compact;

    private static final long IN_MS = 350;
    private static final long OUT_MS = 300;

    /** Called from the chat listener. */
    public static void push(String name, long amount) {
        QUEUE.add(new Alert(name, amount));
    }

    private static long duration() {
        if (compact) return 1600;
        return QUEUE.isEmpty() ? 4500 : 2000; // shorten the big one if more are waiting
    }

    public static void render(GuiGraphicsExtractor g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        long now = System.currentTimeMillis();

        if (current != null && now - startMs >= duration()) current = null;
        if (current == null) {
            Alert next = QUEUE.poll();
            if (next == null) return;
            current = next;
            startMs = now;
            compact = !QUEUE.isEmpty();
            currentSkin = lookupSkin(mc, next.name());
        }

        long t = now - startMs;
        long dur = duration();
        float show;
        if (t < IN_MS) {
            show = easeOutBack(t / (float) IN_MS);
        } else if (t > dur - OUT_MS) {
            float k = Mth.clamp((t - (dur - OUT_MS)) / (float) OUT_MS, 0f, 1f);
            show = 1f - k * k;
        } else {
            show = 1f;
        }

        if (compact) drawCompact(g, mc, t, now, show);
        else drawBig(g, mc, t, now, show);
    }

    // ------------------------------------------------------------------ BIG

    private static void drawBig(GuiGraphicsExtractor g, Minecraft mc, long t, long now, float show) {
        Font font = mc.font;
        int w = 230, h = 58;
        int x = (g.guiWidth() - w) / 2;
        int y = (int) (-h + (h + 8) * show);
        float pulse = 0.5f + 0.5f * (float) Math.sin(t / 120.0);
        float hue = (now / 6 % 360) / 360f;

        // glow
        int glowRgb = Mth.hsvToRgb(hue, 0.8f, 1f);
        for (int i = 6; i >= 1; i--) {
            int a = (int) ((70 - i * 10) * (0.6f + 0.4f * pulse));
            g.fill(x - i, y - i, x + w + i, y + h + i, argb(a, glowRgb));
        }

        // background
        g.fill(x, y, x + w, y + h / 2, 0xF01C1034);
        g.fill(x, y + h / 2, x + w, y + h, 0xF0100820);

        // shine sweep
        int sx = x + (int) ((t % 1400) / 1400f * (w + 60)) - 30;
        int l = Math.max(sx, x), r = Math.min(sx + 16, x + w);
        if (l < r) g.fill(l, y, r, y + h, 0x30FFFFFF);

        // rainbow border
        drawRainbowBorder(g, x, y, w, h, 2, now);

        // face (bounces a little)
        int size = 40;
        int fx = x + 10;
        int fy = y + 9 + (int) Math.round(Math.sin(t / 150.0) * 2);
        g.fill(fx - 2, fy - 2, fx + size + 2, fy + size + 2,
                0xFF000000 | Mth.hsvToRgb(0.12f + 0.04f * pulse, 0.9f, 1f));
        drawFace(g, fx, fy, size, font);

        // texts
        int tx = x + 60;
        int maxW = w - 60 - 8;
        int titleColor = ((t / 200) % 2 == 0) ? 0xFFFFD700 : 0xFFFFFFFF;
        g.text(font, "* PAYMENT RECEIVED *", tx, y + 8, titleColor, true);
        scaledText(g, font, current.name(), tx, y + 20, 1.25f, 0xFFFFA500, maxW);
        scaledText(g, font, "+$" + Money.format(current.amount()), tx, y + 34, 1.7f, 0xFF32CD32, maxW);

        // sparkles flying out
        float cx = x + w / 2f, cy = y + h / 2f;
        for (int i = 0; i < 26; i++) {
            float phase = (i * 0.618f) % 1f;
            float life = ((t * 0.0013f) + phase) % 1f;
            double ang = i * 2.39996;
            float rad = 20 + life * 95;
            int px = (int) (cx + Math.cos(ang) * rad * 1.7f);
            int py = (int) (cy + Math.sin(ang) * rad * 0.65f);
            int a = (int) ((1f - life) * 230 * show);
            int[] cols = {0xFFD700, 0x32CD32, 0xFFA500, 0xFFFFFF};
            int s = (i % 3 == 0) ? 3 : 2;
            g.fill(px, py, px + s, py + s, argb(a, cols[i % cols.length]));
        }
    }

    // -------------------------------------------------------------- COMPACT

    private static void drawCompact(GuiGraphicsExtractor g, Minecraft mc, long t, long now, float show) {
        Font font = mc.font;
        int w = 176, h = 22;
        int x = (g.guiWidth() - w) / 2;
        int y = (int) (-h + (h + 4) * show);

        g.fill(x - 1, y - 1, x + w + 1, y + h + 1,
                0xFF000000 | Mth.hsvToRgb((now / 5 % 360) / 360f, 0.8f, 1f));
        g.fill(x, y, x + w, y + h, 0xF0140C28);

        drawFace(g, x + 3, y + 3, 16, font);

        String amount = "+$" + Money.format(current.amount());
        int amountW = font.width(amount);
        String name = current.name();
        int maxName = w - 22 - amountW - 12;
        while (name.length() > 3 && font.width(name) > maxName) {
            name = name.substring(0, name.length() - 1);
        }
        g.text(font, name, x + 24, y + 7, 0xFFFFA500, true);
        g.text(font, amount, x + w - amountW - 6, y + 7, 0xFF32CD32, true);
    }

    // -------------------------------------------------------------- HELPERS

    private static void drawRainbowBorder(GuiGraphicsExtractor g, int x, int y, int w, int h, int th, long now) {
        float base = (now / 6 % 360) / 360f;
        int segs = 22;
        int segW = (int) Math.ceil(w / (float) segs);
        for (int s = 0; s < segs; s++) {
            int c = 0xFF000000 | Mth.hsvToRgb((base + s / (float) segs) % 1f, 0.85f, 1f);
            int x1 = x + s * segW, x2 = Math.min(x1 + segW, x + w);
            g.fill(x1, y, x2, y + th, c);
            int c2 = 0xFF000000 | Mth.hsvToRgb((base + 0.5f + s / (float) segs) % 1f, 0.85f, 1f);
            g.fill(x1, y + h - th, x2, y + h, c2);
        }
        int segsV = 8;
        int segH = (int) Math.ceil(h / (float) segsV);
        for (int s = 0; s < segsV; s++) {
            int c = 0xFF000000 | Mth.hsvToRgb((base + s / (float) segsV) % 1f, 0.85f, 1f);
            int y1 = y + s * segH, y2 = Math.min(y1 + segH, y + h);
            g.fill(x, y1, x + th, y2, c);
            g.fill(x + w - th, y1, x + w, y2, c);
        }
    }

    private static void drawFace(GuiGraphicsExtractor g, int x, int y, int size, Font font) {
        if (currentSkin != null) {
            PlayerFaceExtractor.extractRenderState(g, currentSkin, x, y, size);
        } else {
            // Skin not available (player not in tab list): grey box with a question mark
            g.fill(x, y, x + size, y + size, 0xFF555555);
            g.text(font, "?", x + size / 2 - 2, y + size / 2 - 4, 0xFFFFFFFF, true);
        }
    }

    /** Draws text bigger/smaller, shrinking automatically so it fits in maxWidth. */
    private static void scaledText(GuiGraphicsExtractor g, Font font, String text,
                                   int x, int y, float scale, int color, int maxWidth) {
        float w = font.width(text) * scale;
        if (w > maxWidth) scale *= maxWidth / w;
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);
        g.text(font, text, 0, 0, color, true);
        g.pose().popMatrix();
    }

    private static PlayerSkin lookupSkin(Minecraft mc, String name) {
        try {
            ClientPacketListener conn = mc.getConnection();
            if (conn == null) return null;
            PlayerInfo info = conn.getPlayerInfo(name);
            if (info == null && name.startsWith(".")) info = conn.getPlayerInfo(name.substring(1));
            return info == null ? null : info.getSkin();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static int argb(int alpha, int rgb) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    private static float easeOutBack(float x) {
        x = Mth.clamp(x, 0f, 1f);
        float c1 = 1.70158f, c3 = c1 + 1f;
        return 1f + c3 * (float) Math.pow(x - 1, 3) + c1 * (float) Math.pow(x - 1, 2);
    }
}
