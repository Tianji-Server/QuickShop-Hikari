package com.ghostchu.quickshop.shop;

import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import com.ghostchu.quickshop.QuickShop;
import com.ghostchu.quickshop.api.shop.Shop;
import com.ghostchu.quickshop.api.shop.ShopControlPanel;
import com.ghostchu.quickshop.api.shop.ShopControlPanelManager;
import com.ghostchu.quickshop.util.ChatSheetPrinter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@AllArgsConstructor
public class SimpleShopControlPanelManager implements ShopControlPanelManager {
    private final QuickShop plugin;
    private final Lock LOCK = new ReentrantLock();
    private final Map<ShopControlPanel, Integer> registry = new LinkedHashMap<>();

    private void resort() {
        if (!LOCK.tryLock()) {
            throw new IllegalStateException("Cannot resort while another thread is sorting");
        }
        LOCK.lock();
        List<Map.Entry<ShopControlPanel, Integer>> list = new ArrayList<>(registry.entrySet());
        list.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));
        registry.clear();
        list.forEach((k) -> registry.put(k.getKey(), k.getValue())); // Re-sort
        LOCK.unlock();
    }

    @Override
    public void register(@NotNull ShopControlPanel panel) {
        LOCK.lock();
        registry.put(panel, panel.getInternalPriority());
        LOCK.unlock();
        resort();
    }

    @Override
    public void unregister(@NotNull ShopControlPanel panel) {
        LOCK.lock();
        registry.remove(panel);
        LOCK.unlock();
        // Doesn't need resort
    }

    @Override
    public void unregister(@NotNull Plugin plugin) {
        LOCK.lock();
        List<ShopControlPanel> pending = new ArrayList<>();
        for (Map.Entry<ShopControlPanel, Integer> entry : registry.entrySet()) {
            if (entry.getKey().getPlugin().equals(plugin)) {
                pending.add(entry.getKey());
            }
        }
        LOCK.unlock();
        pending.forEach(this::unregister);
    }

    @Override
    public void openControlPanel(@NotNull Player player, @NotNull Shop shop) {
        ChatSheetPrinter chatSheetPrinter = new ChatSheetPrinter(player);
        chatSheetPrinter.printHeader();
        chatSheetPrinter.printLine(plugin.text().of(player, "controlpanel.infomation").forLocale());
        List<Component> total = new ArrayList<>();
        for (ShopControlPanel entry : registry.keySet()) {
            try {
                total.addAll(entry.generate(player, shop));
            } catch (Exception e) {
                try {
                    plugin.getLogger().warning("Failed to generate control panel for " + entry.getClass().getName() + ". Contact the developer of the plugin " + entry.getPlugin().getName());
                } catch (Exception e2) {
                    plugin.getLogger().warning("Failed to generate control panel for " + entry.getClass().getName() + "Contact the developer of that plugin");
                }
            }
        }
        total.forEach(chatSheetPrinter::printLine);
        chatSheetPrinter.printFooter();
    }
}