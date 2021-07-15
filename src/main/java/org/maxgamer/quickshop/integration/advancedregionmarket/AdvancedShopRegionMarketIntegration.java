/*
 * This file is a part of project QuickShop, the name is AdvancedShopRegionMarketIntegration.java
 *  Copyright (C) PotatoCraft Studio and contributors
 *
 *  This program is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the
 *  Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.maxgamer.quickshop.integration.advancedregionmarket;

import net.alex9849.arm.events.RestoreRegionEvent;
import net.alex9849.arm.regions.Region;
import net.alex9849.armshopbridge.ArmShopBridge;
import net.alex9849.armshopbridge.adapters.QuickShop4Adapter;
import net.alex9849.armshopbridge.adapters.QuickShopAdapter;
import net.alex9849.armshopbridge.interfaces.IShopPluginAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.integration.QSIntegratedPlugin;
import org.maxgamer.quickshop.shop.Shop;

import java.util.*;
import java.util.logging.Level;

public class AdvancedShopRegionMarketIntegration extends QSIntegratedPlugin implements Listener {
    public AdvancedShopRegionMarketIntegration(QuickShop plugin) {
        super(plugin);
    }

    /**
     * Return the integrated plugin name.
     * For example, Residence
     *
     * @return integrated plugin
     */
    @Override
    public @NotNull String getName() {
        return "AdvancedShopRegionMarket";
    }

    /**
     * Check if a player can create shop here
     *
     * @param player   the player want to create shop
     * @param location shop location
     * @return If you can create shop here
     */
    @Override
    public boolean canCreateShopHere(@NotNull Player player, @NotNull Location location) {
        return true;
    }

    /**
     * Check if a player can trade with shop here
     *
     * @param player   the player want to trade with shop
     * @param location shop location
     * @return If you can trade with shop here
     */
    @Override
    public boolean canTradeShopHere(@NotNull Player player, @NotNull Location location) {
        return true;
    }

    /**
     * Loading logic
     * Execute Stage defined by IntegrationStage
     */
    @Override
    public void load() {
        scanAndUnregister();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPluginLoad(PluginEnableEvent event) {
        if (event.getPlugin().getName().equals("ArmShopBridge")) {
            scanAndUnregister();
        }
    }

    private void scanAndUnregister() {
        try {
            if (Bukkit.getPluginManager().getPlugin("ArmShopBridge") == null) {
                return;
            }
            List<IShopPluginAdapter> adapterList = new ArrayList<>();
            ArmShopBridge.getInstance().getShopPluginAdapters().forEach(adapter -> {
                if (adapter instanceof QuickShopAdapter || adapter instanceof QuickShop4Adapter) {
                    adapterList.add(adapter);
                }
            });
            adapterList.forEach(ArmShopBridge.getInstance().getShopPluginAdapters()::remove);
            Bukkit.getPluginManager().registerEvents(this, plugin);
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING, "Cannot to handle ARM-ShopBridge handlers, disabling...", exception);
            unload();
        }
    }

    /**
     * Unloding logic
     * Will execute when Quickshop unloading
     */
    @Override
    public void unload() {
        HandlerList.unregisterAll(this);
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShopNeedDeletion(RestoreRegionEvent event) {
        Region region = event.getRegion();
        Vector minPoint = region.getRegion().getMinPoint();
        Vector maxPoint = region.getRegion().getMinPoint();
        World world = region.getRegionworld();
        Set<Chunk> chuckLocations = new HashSet<>();

        for (int x = minPoint.getBlockX(); x <= maxPoint.getBlockX() + 16; x += 16) {
            for (int z = minPoint.getBlockZ(); z <= maxPoint.getBlockZ() + 16; z += 16) {
                chuckLocations.add(world.getChunkAt(x >> 4, z >> 4));
            }
        }


        HashMap<Location, Shop> shopMap = new HashMap<>();

        for (Chunk chunk : chuckLocations) {
            Map<Location, Shop> shopsInChunk = plugin.getShopManager().getShops(chunk);
            if (shopsInChunk != null) {
                shopMap.putAll(shopsInChunk);
            }
        }
        for (Location shopLocation : shopMap.keySet()) {
            if (region.getRegion().contains(shopLocation.getBlockX(), shopLocation.getBlockY(), shopLocation.getBlockZ())) {
                Shop shop = shopMap.get(shopLocation);
                if (shop != null) {
                    shop.onUnload();
                    shop.delete(false);
                }
            }
        }
    }
}
