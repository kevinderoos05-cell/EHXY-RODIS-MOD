package com.donutpay;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;

/** The "Profit of the day" box, drawn under the scoreboard on the right side of the screen. */
public final class ProfitHud {
    private ProfitHud() {}

    public static void render(GuiGraphicsExtractor g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        Font font = mc.font;

        long profit = ProfitTracker.getProfit();
        boolean tracking = ProfitTracker.isTracking();

        String title = "Profit of the day";
        String value;
        int valueColor;
        if (profit > 0) {
            value = Money.signed(profit);
            valueColor = 0xFF55FF55;
        } else if (profit < 0) {
            value = "Loss " + Money.signed(profit);
            valueColor = 0xFFFF5555;
        } else {
            value = "$0";
            valueColor = 0xFFFFFFFF;
        }
        String status;
        status = tracking
                ? "Counting..."
                : "Paused - press [" + DonutPayClient.toggleProfitKey.getTranslatedKeyMessage().getString() + "]";
        int statusColor = tracking ? 0xFF55FF55 : 0xFFAAAAAA;

        int w = Math.max(font.width(title), Math.max(font.width(value), font.width(status))) + 10;
        int h = 36;
        int x = g.guiWidth() - w - 1;
        int y = g.guiHeight() / 2 - 20; // default spot if there is no scoreboard

        // Put the box right under the sidebar scoreboard
        try {
            Scoreboard sb = mc.level.getScoreboard();
            Objective obj = sb.getDisplayObjective(DisplaySlot.SIDEBAR);
            if (obj != null) {
                int lines = Math.min(15, sb.listPlayerScores(obj).size());
                y = g.guiHeight() / 2 + (lines * 9) / 3 + 4;
            }
        } catch (RuntimeException ignored) {
            // keep default position
        }

        g.fill(x, y, x + w, y + h, 0x90000000);
        g.fill(x, y, x + 2, y + h, 0xFFFFAA00);
        g.text(font, title, x + 6, y + 4, 0xFFFFAA00, true);
        g.text(font, value, x + 6, y + 14, valueColor, true);
        g.text(font, status, x + 6, y + 25, statusColor, true);
    }
}
