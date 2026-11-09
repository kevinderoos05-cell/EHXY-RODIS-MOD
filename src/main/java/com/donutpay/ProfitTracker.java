package com.donutpay;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Keeps track of your "Profit of the day". Saved to config/donutpay.properties so a restart doesn't lose it. */
public final class ProfitTracker {
    private ProfitTracker() {}

    private static boolean tracking = false;
    private static long profit = 0;

    public static boolean isTracking() { return tracking; }
    public static long getProfit() { return profit; }

    /** Start key: first press starts counting, next press pauses. */
    public static void toggle() {
        tracking = !tracking;
        save();
    }

    /** Reset key: back to $0 (keeps the current counting / paused state). */
    public static void reset() {
        profit = 0;
        save();
    }

    /** Positive = you got paid, negative = you paid someone. Only counts while tracking. */
    public static void add(long amount) {
        if (!tracking) return;
        profit += amount;
        save();
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("donutpay.properties");
    }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) return;
        try (Reader r = Files.newBufferedReader(f)) {
            Properties p = new Properties();
            p.load(r);
            tracking = Boolean.parseBoolean(p.getProperty("tracking", "false"));
            profit = Long.parseLong(p.getProperty("profit", "0"));
        } catch (IOException | NumberFormatException e) {
            System.err.println("[DonutPay] Could not read config: " + e);
        }
    }

    private static void save() {
        try (Writer w = Files.newBufferedWriter(file())) {
            Properties p = new Properties();
            p.setProperty("tracking", Boolean.toString(tracking));
            p.setProperty("profit", Long.toString(profit));
            p.store(w, "DonutPay - Profit of the day");
        } catch (IOException e) {
            System.err.println("[DonutPay] Could not save config: " + e);
        }
    }
}
