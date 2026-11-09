package com.donutpay;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class DonutPayClient implements ClientModInitializer {
    public static final String MOD_ID = "donutpay";

    /** Popup only shows for payments YOU RECEIVE of at least this much (5 billion). */
    public static final long POPUP_MIN = 5_000_000_000L;

    // Colors for the chat messages
    private static final int ORANGE = 0xFFA500;
    private static final int LIME = 0x32CD32;

    public static KeyMapping toggleProfitKey;
    public static KeyMapping resetProfitKey;

    @Override
    public void onInitializeClient() {
        // ---- Keybinds (change them in Options > Controls > Key Binds > DonutPay) ----
        KeyMapping.Category category =
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

        toggleProfitKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.donutpay.toggle_profit", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, category));
        resetProfitKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.donutpay.reset_profit", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, category));

        ProfitTracker.load();

        // ---- Chat: recolor payment messages + trigger popup/profit ----
        ClientReceiveMessageEvents.MODIFY_GAME.register(DonutPayClient::onGameMessage);

        // ---- Keybind handling ----
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleProfitKey.consumeClick()) ProfitTracker.toggle();
            while (resetProfitKey.consumeClick()) ProfitTracker.reset();
        });

        // ---- HUD drawing ----
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(MOD_ID, "payment_popup"), PaymentPopup::render);
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(MOD_ID, "profit_hud"), ProfitHud::render);
    }

    private static Component onGameMessage(Component message, boolean overlay) {
        if (overlay) return message; // ignore action bar text

        PaymentParser.Payment p = PaymentParser.parse(message.getString());
        if (p == null) return message; // not a payment message, leave it alone

        // Profit of the day: received = +, sent = -
        ProfitTracker.add(p.received() ? p.amount() : -p.amount());

        // Popup: ONLY when someone pays YOU and it's 5B or more
        if (p.received() && p.amount() >= POPUP_MIN) {
            PaymentPopup.push(p.name(), p.amount());
        }

        // Rebuild the chat line with colors
        MutableComponent name = Component.literal(p.name()).withStyle(s -> s.withColor(ORANGE));
        MutableComponent amount = Component.literal(
                "$" + p.amountText().replace("$", "").strip()).withStyle(s -> s.withColor(LIME));

        MutableComponent line = Component.literal("");
        if (p.received()) {
            line.append(name).append(Component.literal(" paid you ")).append(amount);
        } else {
            line.append(Component.literal("You paid ")).append(name)
                    .append(Component.literal(" ")).append(amount);
        }
        return line;
    }
}
